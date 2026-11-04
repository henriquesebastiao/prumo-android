package com.henriquesebastiao.prumo.core.network.testing

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue

/**
 * The server's contract (`openapi.json`, copied by `scripts/update-openapi.sh`) and the checks
 * that keep the hand-written DTOs in step with it (docs/decisoes.md, D04).
 */
object OpenApiContract {
    private val spec: JsonObject by lazy { Json.parseToJsonElement(Fixtures.resource("/openapi.json")).jsonObject }

    private fun schemas(): JsonObject = spec["components"]!!.jsonObject["schemas"]!!.jsonObject

    /** The schema [name] of `components.schemas`. */
    fun schema(name: String): JsonObject =
        checkNotNull(schemas()[name]?.jsonObject) { "The server has no schema $name: run scripts/update-openapi.sh" }

    fun properties(name: String): Set<String> = schema(name)["properties"]?.jsonObject?.keys.orEmpty()

    fun required(name: String): Set<String> =
        (schema(name)["required"] as? JsonArray)?.map { (it as JsonPrimitive).content }?.toSet().orEmpty()

    /** The values of an enum schema, or of a property whose type is an inline enum. */
    fun enumValues(name: String, property: String? = null): Set<String> {
        val node = if (property ==
            null
        ) {
            schema(name)
        } else {
            schema(name)["properties"]!!.jsonObject[property]!!.jsonObject
        }
        return (node["enum"] as? JsonArray)?.map { (it as JsonPrimitive).content }?.toSet().orEmpty()
    }

    /**
     * A DTO the app reads: each of its fields exists in [schemaName], and each one it can't do
     * without (no default) is required there, so a valid response always decodes.
     */
    fun assertReads(descriptor: SerialDescriptor, schemaName: String) {
        val properties = properties(schemaName)
        val required = required(schemaName)
        for (i in 0 until descriptor.elementsCount) {
            val field = descriptor.getElementName(i)
            assertTrue("${descriptor.serialName}.$field is not in the server's $schemaName", field in properties)
            if (!descriptor.isElementOptional(i)) {
                assertTrue(
                    "${descriptor.serialName}.$field has no default but is optional in $schemaName",
                    field in required,
                )
            }
        }
    }

    /**
     * A DTO the app sends: each of its fields exists in [schemaName], and it has every field the
     * server requires.
     */
    fun assertWrites(descriptor: SerialDescriptor, schemaName: String) {
        val names = (0 until descriptor.elementsCount).map(descriptor::getElementName).toSet()
        val properties = properties(schemaName)
        names.forEach {
            assertTrue("${descriptor.serialName}.$it is not in the server's $schemaName", it in properties)
        }
        required(schemaName).forEach {
            assertTrue("${descriptor.serialName} lacks $it, required by $schemaName", it in names)
        }
    }
}
