package dev.dres.api.rest.handler.collection

import dev.dres.DRES
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import java.nio.file.FileVisitOption
import java.nio.file.Files
import kotlin.streams.toList

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
/**
 * Lists and returns the media items in the external media item directory.
 */
class ListExternalItemHandler: GetRestHandler<Array<String>> {

    override val apiVersion = "v2"

    @OpenApi(
        summary = "Lists items from the external media collection whose name start with the given string.",
        path = "/api/v2/external/{startsWith}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("startsWith", String::class, "Name starts with.", required = true)
        ],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): Array<String> {
        // TODO https://github.com/javalin/javalin-openapi/issues/178 Apparently, we cannot use the slash-included notation here (https://javalin.io/documentation#endpoint-handlers)
        val startsWith = ctx.pathParamMap()["startsWith"] ?: ""
        val list = Files.walk(DRES.EXTERNAL_ROOT, 1, FileVisitOption.FOLLOW_LINKS).filter {
                Files.isRegularFile(it) && it.toFile().name.startsWith(startsWith)

        }.limit(50).map { it.toFile().name }.toList()
        return list.toTypedArray()
    }

    override val route: String = "external/{startsWith}"
}
