package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.firstOrNull

/**
 * A [GetRestHandler] to dequeue the next [ApiJudgementRequest] that is ready for judgement.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DequeueJudgementHandler(store: TransientEntityStore) : AbstractJudgementHandler(store), GetRestHandler<ApiJudgementRequest> {
    override val route = "evaluation/{evaluationId}/judge/next"

    @OpenApi(
        summary = "Gets the next open submission that is waiting to be judged.",
        path = "/api/v2/evaluation/{evaluationId}/judge/next",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
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
        /* Start transaction. */
        val request = this.store.transactional(false) {
            val evaluationManager = ctx.eligibleManagerForId<RunManager>()
            checkEligibility(ctx, evaluationManager)
            do {
                val validator = evaluationManager.judgementValidators.find { it.hasOpen } ?: break
                val next = validator.next(ctx.sessionToken()!!) ?: break
                val taskDescription = next.second.task().template.textualDescription()
                when (next.second.answers().firstOrNull()?.type()) {
                    AnswerType.TEXT -> {
                        val text = next.second.answers().firstOrNull()?.text ?: continue
                        return@transactional ApiJudgementRequest(next.first, ApiMediaType.TEXT, validator.id, "text", text, taskDescription, null, null)
                    }
                    AnswerType.ITEM -> {
                        val item = next.second.answers().firstOrNull()?.item ?: continue
                        return@transactional ApiJudgementRequest(next.first, item.type().toApi(), validator.id, item.dbCollection().id, item.mediaItemId, taskDescription, null, null)
                    }
                    AnswerType.TEMPORAL -> {
                        val answer = next.second.answers().firstOrNull() ?: continue
                        val item = answer.item ?: continue
                        val start = answer.start ?: continue
                        val end = answer.end ?: continue
                        return@transactional ApiJudgementRequest(next.first, item.type().toApi(), validator.id, item.dbCollection().id, item.mediaItemId, taskDescription, start, end)
                    }
                    else -> continue
                }
            } while (true)
            null
        }
        return request ?: throw ErrorStatusException(202, "There is currently no submission awaiting judgement.", ctx)
    }
}
