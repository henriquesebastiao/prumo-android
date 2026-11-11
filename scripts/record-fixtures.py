#!/usr/bin/env python3
"""Records real responses of a Prumo server as test fixtures of :core:network.

The fixtures feed the network tests (the Home's union, the error format, the health) and the
features' tests (their Home summaries, and each module's DTOs read from real responses). Record them from the design-board data, so they match
the boards: in ~/git/prumo/backend, run `uv run python scripts/e2e_server.py` (port 8001, user
`henrique`, password `prumo-demo-01`, clock frozen at 17/11/2026 14:30 in São Paulo).

Usage: scripts/record-fixtures.py [base url] [username] [password]
Defaults: http://localhost:8001 henrique prumo-demo-01

Only the standard library. The session token is replaced by "test-token" before saving, and the
session the script opens is closed at the end.
"""

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "core" / "network" / "src" / "testFixtures" / "resources" / "fixtures"
DEVICE = "Pixel 9"
FAKE_TOKEN = "test-token"


def call(base, method, path, token=None, body=None):
    """(status, parsed JSON or None, headers) of a request; errors are answers too."""
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(f"{base}{path}", data=data, method=method)
    request.add_header("Accept", "application/json")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            return response.status, (json.loads(raw) if raw else None), dict(response.headers)
    except urllib.error.HTTPError as error:
        raw = error.read()
        return error.code, (json.loads(raw) if raw else None), dict(error.headers)


def save(name, payload):
    target = TARGET / name
    target.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    target.write_text(text, encoding="utf-8")
    print(f"  {name}")


def record_garage(base, token):
    """Veículos (fixtures/garage/): reads, plus one refused odometer reading (nothing is saved)."""

    def get(path):
        status, payload, _ = call(base, "GET", path, token=token)
        assert status == 200, f"{path} answered {status}"
        return payload

    vehicles = get("/api/v1/garage/vehicles")
    save("garage/vehicles.json", vehicles)
    vehicle = next(v for v in vehicles["items"] if v["kind"] == "motorcycle")
    path = f"/api/v1/garage/vehicles/{vehicle['id']}"
    save("garage/vehicle.json", get(path))
    save("garage/odometer.json", get(f"{path}/odometer"))
    maintenances = get(f"{path}/maintenances")
    save("garage/maintenances.json", maintenances)
    save("garage/maintenance.json", get(f"{path}/maintenances/{maintenances['items'][0]['id']}"))
    save("garage/fuelings.json", get(f"{path}/fuelings"))
    save("garage/oil_suggestion.json", get(f"{path}/oil-change-suggestion?km={vehicle['current_km']}"))
    save("garage/workshops.json", get("/api/v1/garage/workshops"))
    save("garage/overview.json", get("/api/v1/garage/overview"))
    status, refused, _ = call(base, "POST", f"{path}/odometer", token=token, body={"km": 1})
    assert status == 422, f"a lower reading answered {status}"
    save("garage/error_odometer_inconsistent.json", refused)


def record_finance(base, token):
    """Finanças (fixtures/finance/): reads, plus two refused writes (nothing is saved)."""

    def get(path):
        status, payload, _ = call(base, "GET", path, token=token)
        assert status == 200, f"{path} answered {status}"
        return payload

    root = "/api/v1/finance"
    save("finance/overview.json", get(f"{root}/overview"))
    accounts = get(f"{root}/accounts")
    save("finance/accounts.json", accounts)
    account = accounts["items"][0]
    save("finance/account.json", get(f"{root}/accounts/{account['id']}"))
    expense = get(f"{root}/categories?kind=expense")
    save("finance/categories_expense.json", expense)
    save("finance/categories_income.json", get(f"{root}/categories?kind=income"))
    transactions = get(f"{root}/transactions?limit=20")
    save("finance/transactions.json", transactions)
    installment = next(t for t in transactions["items"] if t["installment_count"])
    save("finance/transaction_installment.json", get(f"{root}/transactions/{installment['id']}"))
    cards = get(f"{root}/credit-cards")
    save("finance/credit_cards.json", cards)
    card = cards["items"][0]
    save("finance/credit_card.json", get(f"{root}/credit-cards/{card['id']}"))
    save("finance/invoices.json", get(f"{root}/credit-cards/{card['id']}/invoices"))
    closing = card["current_invoice"]["closing_on"]
    save("finance/invoice.json", get(f"{root}/credit-cards/{card['id']}/invoices/{closing}"))
    body = {"kind": "expense", "amount_cents": 100, "occurred_on": "2026-11-17", "description": "x"}
    status, refused, _ = call(base, "POST", f"{root}/transactions", token=token, body=body)
    assert status == 422, f"an expense without account answered {status}"
    save("finance/error_invalid_combination.json", refused)
    used = next(c for c in expense["items"] if c["transactions_count"] > 0)
    status, refused, _ = call(base, "DELETE", f"{root}/categories/{used['id']}", token=token)
    assert status == 422, f"deleting a used category answered {status}"
    save("finance/error_category_in_use.json", refused)


def record_reading(base, token):
    """Livros (fixtures/reading/): reads, plus one refused page (nothing is saved)."""

    def get(path):
        status, payload, _ = call(base, "GET", path, token=token)
        assert status == 200, f"{path} answered {status}"
        return payload

    root = "/api/v1/reading"
    save("reading/books.json", get(f"{root}/books?sort=-created_at"))
    reading = get(f"{root}/books?status=reading")["items"][0]
    path = f"{root}/books/{reading['id']}"
    save("reading/book.json", get(path))
    save("reading/book_notes.json", get(f"{path}/notes"))
    save("reading/notes.json", get(f"{root}/notes"))
    body = {"page": reading["total_pages"] + 1}
    status, refused, _ = call(base, "POST", f"{path}/progress", token=token, body=body)
    assert status == 422, f"a page past the end answered {status}"
    save("reading/error_page_out_of_range.json", refused)


def record_studies(base, token):
    """Estudos (fixtures/studies/): reads, plus a refused plan change and link (nothing is saved)."""

    def get(path):
        status, payload, _ = call(base, "GET", path, token=token)
        assert status == 200, f"{path} answered {status}"
        return payload

    root = "/api/v1/studies"
    save("studies/day.json", get(f"{root}/day"))
    week = get(f"{root}/week")
    save("studies/week.json", week)
    save("studies/week_previous.json", get(f"{root}/week?start={week['previous_start']}"))
    save("studies/overview.json", get(f"{root}/overview"))
    save("studies/areas.json", get(f"{root}/areas?limit=200"))
    subjects = get(f"{root}/subjects?limit=200")
    save("studies/subjects.json", subjects)
    subject = next(item for item in subjects["items"] if item["obsidian_uri"])
    save("studies/subject.json", get(f"{root}/subjects/{subject['id']}"))
    save("studies/topics.json", get(f"{root}/subjects/{subject['id']}/topics"))
    plans = get(f"{root}/plans")
    save("studies/plans.json", plans)
    current = next(item for item in plans["items"] if item["state"] == "current")
    status, locked, _ = call(
        base, "PUT", f"{root}/plans/{current['id']}", token=token, body={"blocks": []}
    )
    assert status in (409, 422), f"a plan in force answered {status}"
    save("studies/error_plan_locked.json", locked)
    body = {"area_id": subject["area_id"], "name": "Link inválido", "obsidian_uri": "https://x.dev"}
    status, refused, _ = call(base, "POST", f"{root}/subjects", token=token, body=body)
    assert status == 422, f"a link that isn't obsidian:// answered {status}"
    save("studies/error_invalid_obsidian_uri.json", refused)


def main():
    base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8001").rstrip("/")
    username = sys.argv[2] if len(sys.argv) > 2 else "henrique"
    password = sys.argv[3] if len(sys.argv) > 3 else "prumo-demo-01"
    print(f"Recording {base} into {TARGET.relative_to(ROOT)}")

    status, health, _ = call(base, "GET", "/api/health")
    assert status == 200, f"health answered {status}"
    save("health.json", health)

    status, wrong, _ = call(
        base,
        "POST",
        "/api/v1/auth/login",
        body={"username": username, "password": "wrong-password", "client": "app"},
    )
    assert status == 401, f"a wrong password answered {status}"
    save("error_invalid_credentials.json", wrong)

    status, invalid, _ = call(
        base,
        "POST",
        "/api/v1/auth/login",
        body={"username": "", "password": "", "client": "app"},
    )
    assert status == 422, f"an empty login answered {status}"
    save("error_validation.json", invalid)

    status, login, _ = call(
        base,
        "POST",
        "/api/v1/auth/login",
        body={"username": username, "password": password, "client": "app", "device_name": DEVICE},
    )
    assert status == 200, f"login answered {status}: {login}"
    token = login["token"]
    save("login.json", {**login, "token": FAKE_TOKEN})

    for name, path in [
        ("me.json", "/api/v1/me"),
        ("preferences.json", "/api/v1/me/preferences"),
        ("modules.json", "/api/v1/modules"),
        ("sessions.json", "/api/v1/auth/sessions"),
        ("dashboard.json", "/api/v1/dashboard"),
    ]:
        status, payload, _ = call(base, "GET", path, token=token)
        assert status == 200, f"{path} answered {status}"
        save(name, payload)

    record_garage(base, token)
    record_finance(base, token)
    record_reading(base, token)
    record_studies(base, token)

    status, missing, _ = call(base, "GET", "/api/v1/auth/sessions", token="not-a-token")
    assert status == 401, f"a bad token answered {status}"
    save("error_unauthorized.json", missing)

    status, _, _ = call(base, "POST", "/api/v1/auth/logout", token=token)
    assert status == 204, f"logout answered {status}"
    print("Done; the recording session was closed.")


if __name__ == "__main__":
    main()
