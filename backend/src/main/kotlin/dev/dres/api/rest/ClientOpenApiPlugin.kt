package dev.dres.api.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import dev.dres.DRES
import io.javalin.openapi.plugin.OpenApiPlugin

class ClientOpenApiPlugin : OpenApiPlugin({
    it
        .withDocumentationPath("/clientapi.json")
        .withDefinitionConfiguration { _, u ->
            u.openApiVersion("3.0.3")
            u.info { info ->
                info.title("DRES Client API")
                    .version(DRES.VERSION)
                    .description("Client API for DRES (Distributed Retrieval Evaluation Server), Version ${DRES.VERSION}")
            }
            u.withCookieAuth("CookieAuth", AccessManager.SESSION_COOKIE_NAME)
        }
        .withDefinitionProcessor { doc: ObjectNode ->

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

                val relevantRoutes = doc.get("paths").fieldNames().asSequence()
                    .filter { path -> blacklist.none { b -> path.contains(b) } }
                    .toList()

                (doc.get("paths") as ObjectNode).retain(relevantRoutes)

                doc.withOpenApi30Nullability().toPrettyString()
        }
})
