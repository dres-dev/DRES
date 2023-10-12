package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to delete an existing [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class DeleteEvaluationTemplateHandler : AbstractEvaluationTemplateHandler(), DeleteRestHandler<SuccessStatus> {
    override val route: String = "template/{templateId}"

    @OpenApi(
        summary = "Deletes the evaluation template with the given template ID.",
        path = "/api/v2/template/{templateId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.", required = true)],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val template = TemplateManager.deleteTemplate(templateIdFromContext(ctx))
        if (template != null) {
            return SuccessStatus("Evaluation template with ID ${template.id} was deleted successfully.")
        } else {
            throw ErrorStatusException(404, "Template not found", ctx)
        }
    }
}

