package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.competition.team.Team
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to list all [Team]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListTeamHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<List<ApiTeam>> {

    override val route: String = "competition/{competitionId}/team/list"

    @OpenApi(
        summary = "Lists all the teams of a specific competition.",
        path = "/api/v1/competition/{competitionId}/team/list",
        pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
        tags = ["Competition"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTeam>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context) = this.store.transactional(true) {
        competitionFromContext(ctx).teams.asSequence().map { it.toApi() }.toList()
    }
}