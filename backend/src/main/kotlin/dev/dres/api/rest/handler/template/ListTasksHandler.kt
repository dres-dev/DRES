package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to list all [DbTaskTemplate]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListTasksHandler() : AbstractEvaluationTemplateHandler(), GetRestHandler<List<ApiTaskTemplate>> {

    override val route: String = "template/{templateId}/task/list"

    @OpenApi(
        summary = "Lists the task templates contained in a specific evaluation template.",
        path = "/api/v2/template/{templateId}/task/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.", required = true)],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTaskTemplate>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context): List<ApiTaskTemplate> {
        val templateId = ctx.pathParam("templateId")
        return TemplateManager.getTemplate(templateId)?.tasks ?: throw ErrorStatusException(404, "Template with id '$templateId' not found.", ctx)
    }
}
