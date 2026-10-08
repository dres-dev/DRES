package dev.dres.api.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Javalin 7 emits nullable schema types in OpenAPI 3.1 style (`"type": ["string", "null"]`), even when the
 * document is declared as OpenAPI 3.0.3. The OpenAPI 3.0 generator then maps these to `any`, so this
 * rewrites them to the 3.0 form (`"type": "string", "nullable": true`) in place.
 *
 * @return The same node, for chaining into a definition processor.
 */
fun ObjectNode.withOpenApi30Nullability(): ObjectNode {
    rewriteNullableTypes(this)
    return this
}

private fun JsonNode.isNullType(): Boolean =
    this is ObjectNode && size() == 1 && get("type")?.asText() == "null"

private fun rewriteNullableTypes(node: JsonNode) {
    if (node is ObjectNode) {
        /* `anyOf: [X, {type: null}]` -> `X` with `nullable: true` (used for nullable references) */
        val anyOf = node.get("anyOf")
        if (anyOf is ArrayNode && anyOf.size() == 2 && anyOf.count { it.isNullType() } == 1) {
            val target = anyOf.first { !it.isNullType() }
            if (target is ObjectNode) {
                node.remove("anyOf")
                node.setAll<JsonNode>(target)
                node.put("nullable", true)
            }
        }

        val type = node.get("type")
        if (type is ArrayNode) {
            val types = type.map { it.asText() }
            val nonNull = types.filter { it != "null" }
            if (types.contains("null") && nonNull.size == 1) {
                node.put("type", nonNull.first())
                node.put("nullable", true)
            }
        }
    }
    node.elements().forEach { rewriteNullableTypes(it) }
}
