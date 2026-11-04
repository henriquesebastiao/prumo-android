package com.henriquesebastiao.prumo.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// The system part of the contract (openapi.json), written by hand with the fields the app uses
// (docs/decisoes.md, D04). ContractTest checks them against the server's schema.

/** `GET /api/health` (schema `Health`). `service` is "prumo" from D44 of the server on. */
@Serializable
data class HealthDto(val service: String? = null, val status: String, val database: String, val version: String)

/** `POST /api/v1/auth/login` (schema `LoginRequest`). The app always sends `client: "app"`. */
@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    val client: String = "app",
    @SerialName("device_name") val deviceName: String? = null,
)

/** Schema `LoginResponse`: [token] comes only for `client: "app"`. */
@Serializable
data class LoginResponseDto(
    val user: UserDto,
    @SerialName("expires_at") val expiresAt: String,
    val token: String? = null,
)

/** Schema `UserRead`. */
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

/** `PATCH /api/v1/me` (schema `UserUpdate`). */
@Serializable
data class UserUpdateDto(@SerialName("display_name") val displayName: String)

/** `PUT /api/v1/me/password` (schema `PasswordChange`). */
@Serializable
data class PasswordChangeDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

/** Schema `PreferencesRead`. */
@Serializable
data class PreferencesDto(
    val theme: String,
    val locale: String = "pt-BR",
    val timezone: String,
    val currency: String,
    @SerialName("enabled_modules") val enabledModules: List<String>,
)

/** `PATCH /api/v1/me/preferences` (schema `PreferencesUpdate`): absent fields don't change. */
@Serializable
data class PreferencesUpdateDto(
    val theme: String? = null,
    @SerialName("enabled_modules") val enabledModules: List<String>? = null,
)

/** Schema `ModuleRead`. */
@Serializable
data class ModuleDto(val id: String, val version: String, val enabled: Boolean, val position: Int)

/** Schema `ModuleList`. */
@Serializable
data class ModuleListDto(val items: List<ModuleDto>)

/** Schema `SessionRead`. */
@Serializable
data class SessionDto(
    val id: String,
    val client: String,
    @SerialName("device_name") val deviceName: String? = null,
    val current: Boolean,
    @SerialName("last_used_at") val lastUsedAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

/** Schema `SessionList`. */
@Serializable
data class SessionListDto(val items: List<SessionDto>)

/**
 * Schema `DashboardRead`. [modules] is a union discriminated by `module`; each summary stays JSON
 * here and is decoded by the module's `PrumoFeature`.
 */
@Serializable
data class DashboardDto(
    val modules: List<JsonObject>,
    val upcoming: List<UpcomingItemDto> = emptyList(),
    val activity: List<ActivityItemDto> = emptyList(),
)

/** Schema `UpcomingItem`. */
@Serializable
data class UpcomingItemDto(
    val module: String,
    val kind: String,
    val on: String,
    val subject: String,
    @SerialName("entity_id") val entityId: String? = null,
    val data: Map<String, JsonElement> = emptyMap(),
)

/** Schema `ActivityItem`. */
@Serializable
data class ActivityItemDto(
    val module: String,
    val kind: String,
    @SerialName("occurred_at") val occurredAt: String,
    val subject: String,
    @SerialName("entity_id") val entityId: String? = null,
    val data: Map<String, JsonElement> = emptyMap(),
)

/** Schema `ErrorResponse`: the format of every error. */
@Serializable
data class ErrorResponseDto(val error: ErrorBodyDto)

/** Schema `ErrorBody`. */
@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String,
    val details: JsonObject? = null,
    val fields: List<ErrorFieldDto>? = null,
)

/** Schema `ErrorField`. */
@Serializable
data class ErrorFieldDto(val path: String, val code: String, val message: String)
