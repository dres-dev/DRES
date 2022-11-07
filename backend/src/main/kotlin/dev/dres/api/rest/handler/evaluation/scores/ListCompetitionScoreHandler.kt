package dev.dres.api.rest.handler.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluation.scores.AbstractScoreHandler
import dev.dres.api.rest.types.evaluation.scores.ApiScoreOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.run.score.scoreboard.ScoreOverview
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore


/**
 * Generates and lists all [ScoreOverview]s for the provided [Evaluation].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListCompetitionScoreHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<List<ApiScoreOverview>> {

    override val route = "score/evaluation/{evaluationId}"

    @OpenApi(
            summary = "Returns the score overviews of a specific evaluation run.",
            path = "/api/v1/score/evaluation/{evaluationId}",
            tags = ["Evaluation Scores"],
            pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<ApiScoreOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiScoreOverview> {
        val manager = ctx.eligibleManagerForId()
        return this.store.transactional (true) {
            manager.scoreboards.map { it.overview().toApi() }
        }
    }
}