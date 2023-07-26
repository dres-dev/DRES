package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isAdmin
import dev.dres.api.rest.types.evaluation.scores.ApiScore
import dev.dres.api.rest.types.evaluation.scores.ApiScoreOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.score.scoreboard.ScoreOverview
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * Generates and lists the [ScoreOverview] for the specified [DbTask].
 *
 *
 * Only valid for [InteractiveRunManager]s.Can only be invoked by admins.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class HistoryTaskScoreHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<ApiScoreOverview> {

    override val route = "score/evaluation/{evaluationId}/history/{taskId}"

    @OpenApi(
        summary = "Returns the overviews of all score boards for the specified task.",
        path = "/api/v2/score/evaluation/{evaluationId}/history/{taskId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation Scores"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam("taskId", String::class, "The task ID", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiScoreOverview::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiScoreOverview {
        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(400, "Parameter 'taskId' is missing!'", ctx)
        if (!ctx.isAdmin()) throw ErrorStatusException(403, "Access denied.", ctx)

        return this.store.transactional(true) {
            val manager = ctx.eligibleManagerForId<InteractiveRunManager>()

            val rac = ctx.runActionContext()
            val scorer = manager.currentTask(rac)?.scorer ?: throw ErrorStatusException(404, "No task run with ID $taskId in run ${manager.id}.", ctx)
            val scores =  scorer.scoreMapFromCache()
            ApiScoreOverview("task",
                manager.currentTaskTemplate(rac).taskGroup.name,
                manager.template.teams.asSequence().map { ApiScore(it.teamId, scores[it.teamId] ?: 0.0) }.toList()
            )
        }
    }
}

