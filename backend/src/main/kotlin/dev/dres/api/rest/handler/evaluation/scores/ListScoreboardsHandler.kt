package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.run.RunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] that returns the names of all available scoreboards for a given run.
 */
class ListScoreboardsHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<List<String>> {
    override val route = "score/evaluation/{evaluationId}/scoreboard/list"

    @OpenApi(
        summary = "Returns a list of available scoreboard names for the given evaluation.",
        path = "/api/v2/evaluation/{evaluationId}/scoreboard/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation Scores"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<String> = this.store.transactional (true) {
        val manager = ctx.eligibleManagerForId<RunManager>()
        manager.scoreboards.map { it.name }
    }
}
