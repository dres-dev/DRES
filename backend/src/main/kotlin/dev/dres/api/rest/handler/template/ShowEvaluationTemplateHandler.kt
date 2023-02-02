package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.template.EvaluationTemplate
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to show an existing [EvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ShowEvaluationTemplateHandler(store: TransientEntityStore) : AbstractEvaluationTemplateHandler(store), GetRestHandler<ApiEvaluationTemplate> {
    override val route: String = "template/{templateId}"

    @OpenApi(
        summary = "Loads the detailed definition of a specific evaluation template.",
        path = "/api/v2/template/{templateId}",
        pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.")],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationTemplate::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context)= this.store.transactional(true) {
       competitionFromContext(ctx).toApi()
    }
}