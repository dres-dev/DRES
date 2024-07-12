package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] to dequeue the next [ApiJudgementRequest] that is ready for judgement.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DequeueJudgementHandler : AbstractJudgementHandler(),
    GetRestHandler<ApiJudgementRequest> {
    override val route = "evaluation/{evaluationId}/judge/next"

    @OpenApi(
        summary = "Gets the next open submission that is waiting to be judged.",
        path = "/api/v2/evaluation/{evaluationId}/judge/next",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam(
            "evaluationId",
            String::class,
            "The evaluation ID.",
            required = true
        )],
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiJudgementRequest::class)]),
            OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiJudgementRequest {
        return nextRequest(ctx) ?: throw ErrorStatusException(
            202,
            "There is currently no submission awaiting judgement.",
            ctx
        )
    }

    private fun nextRequest(ctx: Context): ApiJudgementRequest? {
        val evaluationManager = ctx.eligibleManagerForId<RunManager>()
        checkEligibility(ctx, evaluationManager)
        val validator = evaluationManager.judgementValidators.sortedBy { it.priority }.find { it.hasOpen } ?: return null
        val next = validator.next() ?: return null
        val taskDescription = validator.taskTemplate.textualDescription()
        return ApiJudgementRequest(
            token = next.first,
            validator = validator.id,
            taskDescription = taskDescription,
            answerSet = next.second
        )
    }
}
