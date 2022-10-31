package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.ApiCompetitionOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.competition.CompetitionDescription
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.flatMapDistinct
import kotlinx.dnq.query.size

/**
 * A [GetRestHandler] that can be used to list all [CompetitionDescription]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListCompetitionHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<List<ApiCompetitionOverview>> {
    override val route: String = "competition/list"

    @OpenApi(
        summary = "Lists an overview of all available competitions with basic information about their content.",
        path = "/api/v1/competition/list",
        tags = ["Competition"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiCompetitionOverview>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context) = this.store.transactional(true) {
        CompetitionDescription.all().asSequence().map {
            ApiCompetitionOverview(it.id, it.name, it.description, it.taskGroups.flatMapDistinct { g -> g.tasks }.size(), it.teams.size())
        }.toList()
    }
}
