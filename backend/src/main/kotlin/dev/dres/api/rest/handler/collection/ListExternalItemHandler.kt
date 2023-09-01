package dev.dres.api.rest.handler.collection

import dev.dres.DRES
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.util.stream.Collectors
import kotlin.io.path.name
import kotlin.streams.toList

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
/**
 * Lists and returns the media items in the external media item directory.
 */
class ListExternalItemHandler : AbstractExternalItemHandler(), GetRestHandler<List<String>> {

    @OpenApi(
        summary = "Lists items from the external media collection.",
        path = "/api/v2/external/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<String> {
        // TODO https://github.com/javalin/javalin-openapi/issues/178 Apparently, we cannot use the slash-included notation here (https://javalin.io/documentation#endpoint-handlers)
        try {
            val files = Files.walk(DRES.EXTERNAL_ROOT, 1, FileVisitOption.FOLLOW_LINKS)
            val list = files
                .filter {
                    Files.isRegularFile(it) &&
                            (
                                    it.name.endsWith(".png", ignoreCase = true) ||
                                            it.name.endsWith(".mp4", ignoreCase = true)
                                    )
                }.sorted { o1, o2 -> o1.compareTo(o2) }

            return list.map { it.toFile().name }.collect(Collectors.toList())
        } catch (ioe: IOException) {
            throw ErrorStatusException(
                404,
                "Cannot access external files on '${DRES.EXTERNAL_ROOT}'",
                ctx
            )
        }


    }

    override val route: String = "external/list"
}
