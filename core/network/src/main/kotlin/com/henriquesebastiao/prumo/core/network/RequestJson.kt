package com.henriquesebastiao.prumo.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The JSON of the modules' request bodies. Unlike [NetworkJson], it writes `null`s: on a `PATCH` a
 * `null` clears the field (`docs/api.md` of the server, "Formatos"), and some creations read a
 * `null` as "none" (an oil change without forecast). A field that must stay out is left out of the DTO.
 */
val RequestJson = Json {
    explicitNulls = true
    encodeDefaults = true
}

/** [value] as a request body for Retrofit (`@Body body: JsonObject`). */
fun <T> requestBody(serializer: KSerializer<T>, value: T): JsonObject =
    RequestJson.encodeToJsonElement(serializer, value).jsonObject
