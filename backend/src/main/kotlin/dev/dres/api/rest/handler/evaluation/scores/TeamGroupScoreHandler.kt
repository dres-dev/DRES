package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.evaluation.scores.ApiTeamGroupValue
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.toList

/**
 *
 */
class TeamGroupScoreHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<List<ApiTeamGroupValue>> {
    override val route = "score/evaluation/{evaluationId}/teamGroup/list"

    @OpenApi(
        summary = "Returns team group aggregated values of the current task.",
        path = "/api/v1/score/evaluation/{evaluationId}/teamGroup/list",
        tags = ["Competition Run Scores"],
        pathParams = [
            OpenApiParam("runId", String::class, "ID of the competition run.", required = true),
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTeamGroupValue>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiTeamGroupValue> {
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have a score history.'", ctx)
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        return this.store.transactional(true) {
            val rac = RunActionContext.runActionContext(ctx, manager)
            /* TODO: Not suite sure where the teamGroupAggregator got lost.*/
            //val aggregators = manager.currentTask(rac)?.teamGroupAggregators ?: throw ErrorStatusException(404, "No active task in evaluation ${ctx.evaluationId()}.", ctx)
            //val teamGroups = manager.template.teamsGroups.toList()
            //teamGroups.map { ApiTeamGroupValue(it.name, aggregators[it.teamGroupId]?.lastValue ?: 0.0) }
            emptyList()
        }
    }
}