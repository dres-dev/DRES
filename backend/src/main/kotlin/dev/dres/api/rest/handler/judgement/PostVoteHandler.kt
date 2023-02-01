package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.judgement.ApiVote
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.run.validation.interfaces.VoteValidator
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PostRestHandler] to post an [ApiVote] on a previously dequeued [ApiJudgementRequest].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PostVoteHandler(store: TransientEntityStore): AbstractJudgementHandler(store), PostRestHandler<SuccessStatus> {
    override val route = "evaluation/{evaluationId}/judge/vote"
    override val apiVersion = "v2"


    @OpenApi(
        summary = "Returns a Vote.",
        path = "/api/v2/evaluation/{evaluationId}/judge/vote", methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.")],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiVote::class)]),
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationManager = ctx.eligibleManagerForId()
        val vote = try {
            ctx.bodyAsClass(ApiVote::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        this.store.transactional {
            val validator = evaluationManager.judgementValidators.find { it is VoteValidator && it.isActive } // Get first active vote validator
                ?: throw ErrorStatusException(404, "There is currently no voting going on in evaluation ${evaluationManager.id}.", ctx)
            validator as VoteValidator
            validator.vote(vote.verdict)
        }
        return SuccessStatus("vote received")
    }
}