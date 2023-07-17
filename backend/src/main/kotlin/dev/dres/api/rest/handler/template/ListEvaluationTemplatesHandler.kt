package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.template.ApiEvaluationTemplateOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.template.DbEvaluationTemplate
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.query
import kotlinx.dnq.query.size

/**
 * A [GetRestHandler] that can be used to list all [DbEvaluationTemplate]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListEvaluationTemplatesHandler(store: TransientEntityStore) : AbstractEvaluationTemplateHandler(store), GetRestHandler<List<ApiEvaluationTemplateOverview>> {
    override val route: String = "template/list"

    @OpenApi(
        summary = "Lists an overview of all available competitions with basic information about their content.",
        path = "/api/v2/template/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationTemplateOverview>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = this.store.transactional(true) {
        DbEvaluationTemplate.query(DbEvaluationTemplate::instance eq false).asSequence().map {
            ApiEvaluationTemplateOverview(it.id, it.name, it.description, it.tasks.size(), it.teams.size())
        }.toList()
    }
}
