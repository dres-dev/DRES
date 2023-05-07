package dev.dres.api.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin

class ClientOpenApiPlugin : OpenApiPlugin(OpenApiConfiguration().apply {
    this.info.title = "DRES API (client)"
    this.info.version = "1.0"
    this.info.description = "API for DRES (Distributed Retrieval Evaluation Server), Version 1.0"
    this.documentationPath = "/client-oas"
    this.documentProcessor = {doc ->

        val blacklist = setOf(
            "/external/",
            "/collection",
            "/competition",
            "/run",
            "/audit",
            "/mediaItem",
            "/score",
            "/user/list",
            "/user/session/",
            "/evaluation/admin",
            "/evaluation/template",
            "/evaluation/{evaluationId}/judge",
            "/evaluation/{evaluationId}/vote",
            "/download",
            "/mediaitem",
            "/template"
        )

        val relevantRoutes = doc["paths"].fields().asSequence().filter { blacklist.none { b -> it.key.contains(b) } }.map { it.key }.toList()

        (doc["paths"] as ObjectNode).retain(relevantRoutes)

        doc.toPrettyString()

    }
}) {
}