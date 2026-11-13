package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * A [PostRestHandler] that imports an [ApiEvaluationTemplate] (e.g., a previously downloaded template JSON) as a new template.
 *
 * All IDs contained in the uploaded template are ignored; new tasks, teams and team groups are created.
 */
class ImportEvaluationTemplateHandler : AbstractEvaluationTemplateHandler(), PostRestHandler<ApiEvaluationTemplate> {

    override val route: String = "template/import"

    @OpenApi(
        summary = "Imports an evaluation template (e.g. a previously downloaded template JSON) as a new evaluation template.",
        path = "/api/v2/template/import",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiEvaluationTemplate::class)]),
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationTemplate::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiEvaluationTemplate {
        val template = try {
            ctx.bodyAsClass(ApiEvaluationTemplate::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid template file: ${e.message}", ctx)
        }

        return try {
            TemplateManager.importTemplate(template)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Could not import template: ${e.message}", ctx)
        }
    }
}
