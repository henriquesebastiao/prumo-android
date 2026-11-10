package com.henriquesebastiao.prumo.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The signed-in user (`GET /me`). The username is set on the server and can't be changed. */
data class User(val id: String, val username: String, val displayName: String, val isAdmin: Boolean)

/** The theme, kept in the account (`theme` in `/me/preferences`): it follows the user across devices. */
enum class ThemeMode(val wire: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        /** The mode for [wire]; an unknown value (a newer server) falls back to [System]. */
        fun fromWire(wire: String?): ThemeMode = entries.firstOrNull { it.wire == wire } ?: System
    }
}

/**
 * The account's preferences (`GET /me/preferences`). [timezone] is where "today" and "this month"
 * are (S3 of the server), [currency] the ISO code money is written in (F9), [enabledModules] the
 * enabled module ids in the user's order.
 */
data class Preferences(
    val theme: ThemeMode,
    val timezone: String,
    val currency: String,
    val enabledModules: List<String>,
) {
    /** [timezone] as a zone; an id the device doesn't know falls back to the device's. */
    val zone: ZoneId
        get() = try {
            ZoneId.of(timezone)
        } catch (_: java.time.DateTimeException) {
            ZoneId.systemDefault()
        }

    /** Today in the user's zone. */
    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(zone).toLocalDate()

    companion object {
        val Default = Preferences(ThemeMode.System, "America/Sao_Paulo", "BRL", emptyList())
    }
}

/** A module installed on the server and the user's choice for it (`GET /modules`). */
data class InstalledModule(val id: String, val version: String, val enabled: Boolean, val position: Int)

/** Who opened a session: the web (cookie) or an app (bearer). */
enum class SessionClient(val wire: String) {
    Web("web"),
    App("app"),
    ;

    companion object {
        fun fromWire(wire: String?): SessionClient = entries.firstOrNull { it.wire == wire } ?: Web
    }
}

/** One of the user's active sessions (`GET /auth/sessions`). [current] is this device's. */
data class DeviceSession(
    val id: String,
    val client: SessionClient,
    val deviceName: String?,
    val current: Boolean,
    val lastUsedAt: Instant,
    val expiresAt: Instant,
)

/**
 * What testing a server address found (`GET /api/health`), as the server screen tells it. [Found]
 * carries the time the health took to answer, for the "Resposta" tile.
 */
sealed interface ServerCheck {
    data class Found(val baseUrl: String, val version: String, val latencyMillis: Long) : ServerCheck

    /** The server answered `503`: it runs, but its database doesn't. */
    data class DatabaseDown(val baseUrl: String, val version: String) : ServerCheck

    /** Something answered, but it isn't a Prumo server. */
    data class NotPrumo(val baseUrl: String) : ServerCheck

    /** Nothing answered at any candidate address; [cause] says what to check. */
    data class Unreachable(val baseUrl: String, val cause: UnreachableCause) : ServerCheck

    /** The text can't be an address. */
    data object InvalidAddress : ServerCheck
}

/**
 * The Home (`GET /dashboard`): one summary per enabled module in the user's order, what is due
 * soon and what was recorded lately.
 */
data class Dashboard(
    val summaries: List<ModuleSummary>,
    val upcoming: List<UpcomingItem>,
    val activity: List<ActivityItem>,
)

/**
 * A module's Home summary, as JSON: each module's `PrumoFeature` decodes its own (the API's
 * discriminated union by `module`). A module the app doesn't know is skipped by the Home.
 */
data class ModuleSummary(val module: String, val json: kotlinx.serialization.json.JsonObject)

/**
 * Something due on a day ("Vem aí"): an oil change, an invoice closing. [kind] is a stable code
 * translated by the module (`garage.oil_change`); [data] holds its values, documented per kind.
 */
data class UpcomingItem(
    val module: String,
    val kind: String,
    val on: LocalDate,
    val subject: String,
    val entityId: String?,
    val data: Map<String, kotlinx.serialization.json.JsonPrimitive>,
)

/** A recent record ("Atividade recente"), newest first. */
data class ActivityItem(
    val module: String,
    val kind: String,
    val occurredAt: Instant,
    val subject: String,
    val entityId: String?,
    val data: Map<String, kotlinx.serialization.json.JsonPrimitive>,
)

/** A text value of an item's `data` ([UpcomingItem], [ActivityItem]), or null when absent or not text. */
fun Map<String, kotlinx.serialization.json.JsonPrimitive>.text(key: String): String? =
    this[key]?.takeIf { it.isString }?.content

/** A whole number of an item's `data`, or null when absent, `null` or not a number. */
fun Map<String, kotlinx.serialization.json.JsonPrimitive>.long(key: String): Long? =
    this[key]?.takeUnless { it.isString }?.content?.toLongOrNull()

/** A decimal of an item's `data` (a pace), or null when absent, `null` or not a number. */
fun Map<String, kotlinx.serialization.json.JsonPrimitive>.double(key: String): Double? =
    this[key]?.takeUnless { it.isString }?.content?.toDoubleOrNull()

/** The part of the day the Home greets: "Bom dia", "Boa tarde", "Boa noite". */
enum class DayPart {
    Morning,
    Afternoon,
    Night,
    ;

    companion object {
        private const val MORNING_FROM = 5
        private const val AFTERNOON_FROM = 12
        private const val NIGHT_FROM = 18

        fun of(hour: Int): DayPart = when (hour) {
            in MORNING_FROM until AFTERNOON_FROM -> Morning
            in AFTERNOON_FROM until NIGHT_FROM -> Afternoon
            else -> Night
        }
    }
}

/**
 * What this device keeps for itself (DataStore; D12): pure black and dynamic color are of the
 * device, as are the four actions pinned to the Home's toolbar (ids of `QuickAction`s).
 */
data class DevicePreferences(
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = true,
    val pinnedActions: List<String>? = null,
)
