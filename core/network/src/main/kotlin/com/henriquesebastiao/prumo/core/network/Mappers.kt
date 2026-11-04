package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.ActivityItem
import com.henriquesebastiao.prumo.core.model.Dashboard
import com.henriquesebastiao.prumo.core.model.DeviceSession
import com.henriquesebastiao.prumo.core.model.InstalledModule
import com.henriquesebastiao.prumo.core.model.ModuleSummary
import com.henriquesebastiao.prumo.core.model.Preferences
import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.model.UpcomingItem
import com.henriquesebastiao.prumo.core.model.User
import com.henriquesebastiao.prumo.core.network.dto.ActivityItemDto
import com.henriquesebastiao.prumo.core.network.dto.DashboardDto
import com.henriquesebastiao.prumo.core.network.dto.ModuleDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesDto
import com.henriquesebastiao.prumo.core.network.dto.SessionDto
import com.henriquesebastiao.prumo.core.network.dto.UpcomingItemDto
import com.henriquesebastiao.prumo.core.network.dto.UserDto
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

// DTOs never leave :core:network: repositories get these models.

fun UserDto.toModel() = User(id = id, username = username, displayName = displayName, isAdmin = isAdmin)

fun PreferencesDto.toModel() = Preferences(
    theme = ThemeMode.fromWire(theme),
    timezone = timezone,
    currency = currency,
    enabledModules = enabledModules,
)

fun ModuleDto.toModel() = InstalledModule(id = id, version = version, enabled = enabled, position = position)

fun SessionDto.toModel() = DeviceSession(
    id = id,
    client = SessionClient.fromWire(client),
    deviceName = deviceName,
    current = current,
    lastUsedAt = parseInstant(lastUsedAt),
    expiresAt = parseInstant(expiresAt),
)

/** The Home. A summary without a `module` (the base schema of a server without modules) is skipped. */
fun DashboardDto.toModel() = Dashboard(
    summaries = modules.mapNotNull { json ->
        (json["module"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { ModuleSummary(it, json) }
    },
    upcoming = upcoming.map { it.toModel() },
    activity = activity.map { it.toModel() },
)

fun UpcomingItemDto.toModel() = UpcomingItem(
    module = module,
    kind = kind,
    on = LocalDate.parse(on),
    subject = subject,
    entityId = entityId,
    data = data.primitives(),
)

fun ActivityItemDto.toModel() = ActivityItem(
    module = module,
    kind = kind,
    occurredAt = parseInstant(occurredAt),
    subject = subject,
    entityId = entityId,
    data = data.primitives(),
)

/**
 * An ISO 8601 date-time from the API (`2026-11-02T18:30:00Z`, maybe with an offset or fractions).
 * `Instant.parse` on older Android only reads `Z`.
 */
fun parseInstant(text: String): Instant = OffsetDateTime.parse(text).toInstant()

private fun Map<String, JsonElement>.primitives(): Map<String, JsonPrimitive> =
    mapNotNull { (key, value) -> (value as? JsonPrimitive)?.let { key to it } }.toMap()
