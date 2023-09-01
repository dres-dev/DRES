package dev.dres.api.rest.handler.collection

import dev.dres.DRES
import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import java.io.IOException
import java.nio.file.Files

class DeleteExternalItemHandler: DeleteRestHandler<SuccessStatus>, AbstractExternalItemHandler(){

    @OpenApi(
        summary = "Deletes the external item with this name, as far as it exists.",
        path = "/api/v2/external/{name}",
        pathParams = [OpenApiParam("name", String::class, "Filename of external item to delete", required = true)],
        tags = ["Collection"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)], description = "On success (the item is deleted)"),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)], description = "For caller error"),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)], description = "If the caller has not the appropriate rights. Requires role admin"),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)], description = "If no such external file exists"),
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val filename = ctx.pathParamMap()["name"] ?: throw ErrorStatusException(400, "No filename given", ctx)
        try {
            Files.delete(DRES.EXTERNAL_ROOT.resolve(filename))
            return SuccessStatus("Successfully deleted $filename")
        }catch(nsf: NoSuchFileException){
            throw ErrorStatusException(404, "The file $filename does not exist", ctx)
        }catch(io: IOException){
            throw ErrorStatusException(500, "Server error: ${io.message}", ctx)
        }
    }

    override val route: String = "external/{name}"

}
