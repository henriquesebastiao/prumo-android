package com.henriquesebastiao.prumo.core.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Lenient on purpose: the v1 API only grows (`docs/api.md`), so new fields, missing optional ones
 * and enum values the app doesn't know yet must not break it.
 */
val NetworkJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}

/**
 * [element] decoded with [deserializer], or null when it doesn't fit (a module's Home summary from a
 * server the app doesn't understand yet): the card shows without data instead of the app failing.
 */
fun <T> decodeOrNull(deserializer: DeserializationStrategy<T>, element: JsonElement?): T? {
    if (element == null) return null
    return try {
        NetworkJson.decodeFromJsonElement(deserializer, element)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
