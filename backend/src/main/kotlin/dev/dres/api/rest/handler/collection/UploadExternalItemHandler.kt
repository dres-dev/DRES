package dev.dres.api.rest.handler.collection

import dev.dres.DRES
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import java.io.IOException
import java.nio.file.OpenOption
import kotlin.io.path.writeBytes

/**
 * API endpoint to upload an external image.
 * @author Loris Sauter
 * @version 1.0
 */
class UploadExternalItemHandler : PostRestHandler<SuccessStatus>, AccessManagedRestHandler{

    override val apiVersion = "v2"

    @OpenApi(
        summary = "Receives a new external (media) item to be used in query hints.",
        path = "/api/v2/external/upload/{name}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        requestBody = OpenApiRequestBody([
            OpenApiContent(mimeType = "image/png", format = "binary", type = "string"),
            OpenApiContent(mimeType = "video/mp4", format = "binary", type = "string"),
        ],required = true, description = "The file to upload."),
        pathParams = [OpenApiParam("name",
            String::class,
            "The name of the file",
            required = true,
            allowEmptyValue = false)]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val fileName = ctx.pathParamMap()["name"] ?: throw ErrorStatusException(400, "No filename given", ctx)
        try {
            val toStorePath = DRES.EXTERNAL_ROOT.resolve(fileName)
            toStorePath.writeBytes(ctx.bodyAsBytes())
            return SuccessStatus("The file $fileName was successfully processed.")
        } catch (ioe: IOException) {
            throw ErrorStatusException(404, "Cannot access external files on '${DRES.EXTERNAL_ROOT}'", ctx)
        }


    }

    override val permittedRoles = setOf(ApiRole.ADMIN)

    override val route: String = "external/upload/{name}"
}
