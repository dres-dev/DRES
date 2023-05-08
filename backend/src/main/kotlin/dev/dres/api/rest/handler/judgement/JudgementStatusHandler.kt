package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.judgement.ApiJudgementValidatorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.run.RunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore


/**
 * A [GetRestHandler] to list the status of all judgement validators for a given evaluation.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class JudgementStatusHandler(store: TransientEntityStore): AbstractJudgementHandler(store), GetRestHandler<List<ApiJudgementValidatorStatus>> {
    override val permittedRoles = setOf(ApiRole.VIEWER)
    override val route = "evaluation/{evaluationId}/judge/status"
    override val apiVersion = "v2"

    @OpenApi(
            summary = "Retrieves the status of all judgement validators.",
            path = "/api/v2/evaluation/{evaluationId}/judge/status",
        operationId = OpenApiOperation.AUTO_GENERATE,
            pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<ApiJudgementValidatorStatus>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiJudgementValidatorStatus> = this.store.transactional(true) {
        val evaluationManager = ctx.eligibleManagerForId<RunManager>()
        checkEligibility(ctx, evaluationManager)
        evaluationManager.judgementValidators.map { ApiJudgementValidatorStatus(it.id, it.pending, it.open) }
    }
}


