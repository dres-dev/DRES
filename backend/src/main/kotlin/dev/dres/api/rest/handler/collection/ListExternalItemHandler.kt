package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.Config
import io.javalin.http.Context
import io.javalin.openapi.*
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
/**
 * Lists and returns the media items in the external media item directory.
 */
class ListExternalItemHandler(config: Config) : GetRestHandler<Array<String>> {

    override val apiVersion = "v2"

    /** Path to the directory that contains the external items. */
    val path = Paths.get(config.externalPath)

    init {
        /* Check if directory exists and create it, if it doesn't. */
        if (!Files.exists(this.path)) {
            Files.createDirectories(this.path)
        }
    }

    @OpenApi(
        summary = "Lists items from the external media collection whose name start with the given string.",
        path = "/api/v2/external/<startsWith>",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("startsWith", String::class, "Name starts with.", required = false)
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
        val startsWith = ctx.pathParamMap()["startsWith"] ?: ""
        val list = Files.walk(this.path, 1, FileVisitOption.FOLLOW_LINKS).filter {
            Files.isRegularFile(it) && it.fileName.toString().startsWith(startsWith)
        }.limit(50).map { it.toString() }.toList()
        return list.toTypedArray()
    }

    override val route: String = "external/<startsWith>"
}