package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.template.team.DbTeam
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to list all [DbTeam]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListTeamHandler(store: TransientEntityStore) : AbstractEvaluationTemplateHandler(store), GetRestHandler<List<ApiTeam>> {

    override val route: String = "template/{templateId}/team/list"

    @OpenApi(
        summary = "Lists all the teams of a specific evaluation template.",
        path = "/api/v2/template/{templateId}/team/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.", required = true)],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTeam>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context) = this.store.transactional(true) {
        evaluationTemplateFromContext(ctx).teams.asSequence().map { it.toApi() }.toList()
    }
}
