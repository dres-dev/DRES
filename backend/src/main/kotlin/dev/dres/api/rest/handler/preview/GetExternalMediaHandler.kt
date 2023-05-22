package dev.dres.api.rest.handler.preview

import dev.dres.DRES
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.utilities.extensions.errorResponse
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * An [GetRestHandler] used to access files from the external media collection.
 *
 * @author Loris sauter
 * @version 1.0.0
 */
class GetExternalMediaHandler : GetRestHandler<Any>, AccessManagedRestHandler {

    // not used, as the medium is directly served
    override fun doGet(ctx: Context): Any = ""

    /** All [GetMediaHandler] can be used by [ApiRole.VIEWER], [ApiRole.PARTICIPANT] and [ApiRole.ADMIN]. */
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.ADMIN)

    override val route = "media/external/{file}"

    /**
     * The API version, currently `v2`
     */
    override val apiVersion = "v2"

    @OpenApi(
        summary = "Returns the requested external mediu, if present",
        path = "/api/v2/media/external/{file}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("file", String::class, "The file name including suffix")
        ],
        tags = ["Media"],
        responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse(
            "404"
        )],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        /* Extract parameter */
        val file = ctx.pathParamMap()["file"]
        if (file == null) {
            ctx.errorResponse(400, "Missing parameter file")
            return
        }

        /* Lookup */
        val path = DRES.EXTERNAL_ROOT.resolve(Path(file))
        if (path == null || !Files.exists(path)) {
            ctx.errorResponse(404, "External file with name ${file} was not found")
        }

        ctx.contentType(ContentType.getContentTypeByExtension(path.toFile().extension)!!)
        try {
            ctx.streamFile(path)
        } catch (e: org.eclipse.jetty.io.EofException) {
            //is triggered by a client abruptly stopping playback, can be safely ignored
        }
    }
}
