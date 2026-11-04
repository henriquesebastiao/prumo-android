package com.henriquesebastiao.prumo.ui

import com.henriquesebastiao.prumo.core.feature.HomeContext
import com.henriquesebastiao.prumo.core.model.DayPart
import com.henriquesebastiao.prumo.core.model.DeviceSession
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.model.User
import com.henriquesebastiao.prumo.feature.finance.FinanceFeature
import com.henriquesebastiao.prumo.feature.garage.GarageFeature
import com.henriquesebastiao.prumo.feature.reading.ReadingFeature
import com.henriquesebastiao.prumo.system.home.ActionGroup
import com.henriquesebastiao.prumo.system.home.HomeModuleCard
import com.henriquesebastiao.prumo.system.home.HomeUiState
import com.henriquesebastiao.prumo.system.settings.ModuleItem
import com.henriquesebastiao.prumo.system.settings.ModulesUiState
import com.henriquesebastiao.prumo.system.settings.SessionsUiState
import com.henriquesebastiao.prumo.system.settings.SettingsUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** The design boards' data, for previews and screen tests (the same as `e2e_server.py` of the server). */
object PreviewData {
    private val finance = FinanceFeature()
    private val garage = GarageFeature()
    private val reading = ReadingFeature()
    private val today = LocalDate.of(2026, 11, 2)

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    val home = HomeUiState(
        loading = false,
        greeting = DayPart.Afternoon,
        date = today,
        cards = listOf(
            HomeModuleCard(
                finance,
                json(
                    """{"module":"finance","month":"2026-11","total_balance_cents":874235,"expense_cents":148290,
                    "next_invoice":{"card":{"id":"1","name":"Nubank","color":"violet"},
                    "invoice":{"closing_on":"2026-12-03","amount_cents":106422,"status":"open"}}}""",
                ),
            ),
            HomeModuleCard(
                garage,
                json(
                    """{"module":"garage","vehicles_count":2,"next_due":{"vehicle":{"id":"1","name":"Honda CG 160 Fan",
                    "kind":"motorcycle"},"status":"due_soon","primary":"km","remaining_km":180,"remaining_days":65,
                    "used_percent":82}}""",
                ),
            ),
            HomeModuleCard(
                reading,
                json(
                    """{"module":"reading","books_count":3,"current":{"book":{"id":"1","title":"Código Limpo",
                    "author":"Robert C. Martin"},"current_page":212,"total_pages":425,"progress_percent":50}}""",
                ),
            ),
        ),
        context = HomeContext("BRL", today),
        pinned = listOf(
            finance.quickActions[0],
            garage.quickActions[0],
            garage.quickActions[1],
            reading.quickActions[0],
        ),
        groups = listOf(finance, garage, reading).map { ActionGroup(it, it.quickActions) },
        loadedAt = LocalTime.of(13, 5),
        server = "prumo.minhacasa.net",
    )

    val user = User("1", "marina", "Marina Costa", isAdmin = true)

    val settings = SettingsUiState(
        user = user,
        preferences = Preferences(ThemeMode.System, "America/Sao_Paulo", "BRL", listOf("finance", "garage", "reading")),
        enabledModules = listOf(finance.nameRes, garage.nameRes, reading.nameRes),
        sessionsCount = 3,
        server = "prumo.minhacasa.net",
        serverReachable = true,
    )

    val modules = ModulesUiState(
        loading = false,
        items = listOf(ModuleItem(finance, true), ModuleItem(garage, true), ModuleItem(reading, false)),
    )

    val sessions = SessionsUiState(
        loading = false,
        sessions = listOf(
            DeviceSession("1", SessionClient.App, "Pixel 8", true, Instant.parse("2026-11-17T17:30:00Z"), Instant.MAX),
            DeviceSession(
                "2",
                SessionClient.Web,
                "Firefox · Linux",
                false,
                Instant.parse("2026-11-17T12:14:00Z"),
                Instant.MAX,
            ),
            DeviceSession(
                "3",
                SessionClient.Web,
                "Chrome · Windows",
                false,
                Instant.parse("2026-10-21T21:40:00Z"),
                Instant.MAX,
            ),
        ),
        zone = ZoneId.of("America/Sao_Paulo"),
    )
}
