package dev.dres.api.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import dev.dres.DRES
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.openapi.plugin.SecurityComponentConfiguration

class ClientOpenApiPlugin : OpenApiPlugin(OpenApiPluginConfiguration()
    .withDocumentationPath("/client-oas")
    .withDefinitionConfiguration { _, u ->
        u.withOpenApiInfo { t ->
            t.title = "DRES Client API"
            t.version = DRES.VERSION
            t.description = "Client API for DRES (Distributed Retrieval Evaluation Server), Version ${DRES.VERSION}"
        }
        u.withSecurity(
            SecurityComponentConfiguration()
                .withSecurityScheme("CookieAuth", CookieAuth(AccessManager.SESSION_COOKIE_NAME))
        )
        u.withDefinitionProcessor { doc ->

            val blacklist = setOf(
                "/external/",
                "/collection",
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
                "/evaluation/{evaluationId}/submission",
                "/evaluation/{evaluationId}/task",
                "/evaluation/{evaluationId}/{taskId}",
                "/download",
                "/mediaitem",
                "/template",
                "/preview",
                "/status/info"
            )

            val relevantRoutes =
                doc["paths"].fields().asSequence().filter { blacklist.none { b -> it.key.contains(b) } }.map { it.key }
                    .toList()

            (doc["paths"] as ObjectNode).retain(relevantRoutes)

            doc.toPrettyString()

        }
    })
