package com.henriquesebastiao.prumo.core.model

/** An enum value as the API sends it (`"due_soon"`, `"card_payment"`). */
interface Wire {
    val wire: String
}

/**
 * The entry of [T] whose [Wire.wire] is [value], or [fallback] for a value the app doesn't know yet:
 * the v1 API only grows, and a newer server may send a new one (docs/api.md of the server).
 */
inline fun <reified T> wireOf(value: String?, fallback: T): T where T : Enum<T>, T : Wire =
    enumValues<T>().firstOrNull { it.wire == value } ?: fallback
