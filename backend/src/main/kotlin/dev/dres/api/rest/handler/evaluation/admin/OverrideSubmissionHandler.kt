package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.evaluation.ApiVerdictStatus
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PatchRestHandler] used to overwrite [VerdictStatus] information.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class OverrideSubmissionHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PatchRestHandler<ApiSubmission> {
    override val route: String = "evaluation/admin/{evaluationId}/submission/override"

    @OpenApi(
        summary = "Override the submission status for a given submission.",
        path = "/api/v1/evaluation/admin/{evaluationId}/submission/override",
        methods = [HttpMethod.PATCH],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiSubmission::class)]),
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiSubmission::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): ApiSubmission {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)

        /* TODO: Make this work for batched submissions! */

        /* Extract HTTP body. */
        val submissionInfo = try {
            ctx.bodyAsClass<ApiSubmission>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }
        /* Perform sanity check. */

        if (submissionInfo.verdicts.first().status == ApiVerdictStatus.INDETERMINATE ) {
            throw ErrorStatusException(400, "Submission status can not be set to INDETERMINATE.", ctx)
        }

        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)

            /* Sanity check to see, whether the submission exists */
            if (evaluationManager.allSubmissions(rac).none { it.id == submissionInfo.id }) {
                throw ErrorStatusException(404, "The given submission $submissionInfo was not found.", ctx)
            }
            if (evaluationManager.updateSubmission(rac, submissionInfo.id, submissionInfo.verdicts.first().status.status)) {
                val submission = evaluationManager.allSubmissions(rac).single { it.id == submissionInfo.id }
                AuditLogger.overrideSubmission(submission, AuditLogSource.REST, ctx.sessionId())
                submission.toApi()
            } else {
                throw ErrorStatusException(500, "Could not update the submission. Please see the backend's log.", ctx)
            }
        }
    }
}