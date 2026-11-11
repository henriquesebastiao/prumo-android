package com.henriquesebastiao.prumo.core.network.testing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Real responses of the server, recorded by `scripts/record-fixtures.py` from the
 * design-board data (`backend/scripts/e2e_server.py` of the server). Never written by hand.
 */
object Fixtures {
    /** The text of `fixtures/[name]` (`dashboard.json`, `login.json`…). */
    fun read(name: String): String = resource("/fixtures/$name")

    /**
     * The `id` of the item of `fixtures/[name]` (a list `{"items": [...]}`) whose [field] is [value]:
     * the ids change with each recording, so tests find them by what the data says.
     */
    fun idOf(name: String, field: String, value: String): String =
        Json.parseToJsonElement(read(name)).jsonObject.getValue("items").jsonArray
            .map { it.jsonObject }
            .first { it[field]?.jsonPrimitive?.content == value }
            .getValue("id").jsonPrimitive.content

    internal fun resource(path: String): String =
        checkNotNull(Fixtures::class.java.getResource(path)) { "Missing test resource $path" }.readText()
}
