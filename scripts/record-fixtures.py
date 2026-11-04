#!/usr/bin/env python3
"""Records real responses of a Prumo server as test fixtures of :core:network.

The fixtures feed the network tests (the Home's union, the error format, the health) and the
features' tests of their Home summaries. Record them from the design-board data, so they match
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
    TARGET.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    (TARGET / name).write_text(text, encoding="utf-8")
    print(f"  {name}")


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

    status, missing, _ = call(base, "GET", "/api/v1/auth/sessions", token="not-a-token")
    assert status == 401, f"a bad token answered {status}"
    save("error_unauthorized.json", missing)

    status, _, _ = call(base, "POST", "/api/v1/auth/logout", token=token)
    assert status == 204, f"logout answered {status}"
    print("Done; the recording session was closed.")


if __name__ == "__main__":
    main()
