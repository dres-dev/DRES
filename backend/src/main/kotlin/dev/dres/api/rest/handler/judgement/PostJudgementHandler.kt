package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.judgement.ApiJudgement
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PostRestHandler] to post an [ApiJudgement] for a previously dequeued [ApiJudgementRequest].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PostJudgementHandler(store: TransientEntityStore): AbstractJudgementHandler(store), PostRestHandler<SuccessStatus> {
    override val route = "evaluation/{evaluationId}/judge"

    @OpenApi(
        summary = "Endpoint to post a judgement for a previously detached judgement request.",
        path = "/api/v2/run/{runId}/judge", methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.")],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiJudgement::class)]),
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("408", [OpenApiContent(ErrorStatus::class)], "On timeout: Judgement took too long"),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        /* Obtain manager and check if any submissions are waiting for judgement. */
        val evaluationManager = ctx.eligibleManagerForId()
        val judgement = try {
            ctx.bodyAsClass(ApiJudgement::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Start transaction. */
        this.store.transactional {
            checkEligibility(ctx, evaluationManager)
            val validator = evaluationManager.judgementValidators.find { it.id == judgement.validator }
                ?: throw ErrorStatusException(404, "No matching task found for validator ${judgement.validator}.", ctx)
            try {
                validator.judge(judgement.token, judgement.verdict.toDb())
            } catch (ex: JudgementTimeoutException) {
                throw ErrorStatusException(408, ex.message!!, ctx)
            }
            AuditLogger.judgement(evaluationManager.id, validator, judgement.token, judgement.verdict.toDb(), DbAuditLogSource.REST, ctx.sessionToken())
        }
        return SuccessStatus("Verdict ${judgement.verdict} received and accepted. Thanks!")
    }
}
