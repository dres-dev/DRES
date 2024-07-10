package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.evaluation.ApiOverrideAnswerSetVerdictDto
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.any
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first

/**
 * A [PatchRestHandler] used to overwrite [DbVerdictStatus] information.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class OverrideAnswerSetVerdictHandler(private val store: TransientEntityStore): AbstractEvaluationAdminHandler(), PatchRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/override/{answerSetId}"

    @OpenApi(
        summary = "Override the verdict status of an AnswerSet.",
        path = "/api/v2/evaluation/admin/{evaluationId}/override/{answerSetId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.PATCH],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("answerSetId", String::class, "The ID of the AnswerSet.", required = true, allowEmptyValue = false)
        ],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiOverrideAnswerSetVerdictDto::class)]),
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val answerSetId = ctx.pathParamMap()["answerSetId"] ?: throw ErrorStatusException(400, "Parameter 'answerSetId' is missing!'", ctx)
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)

        /* Extract HTTP body. */
        val apiVerdictStatus = try {
            ctx.bodyAsClass<ApiOverrideAnswerSetVerdictDto>().verdict
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Perform sanity check. */
        if (apiVerdictStatus == ApiVerdictStatus.INDETERMINATE ) {
            throw ErrorStatusException(400, "Submission status can not be set to INDETERMINATE.", ctx)
        }

        return this.store.transactional {
            val rac = ctx.runActionContext()

            val dbSubmission = evaluationManager.allSubmissions().find { submission -> submission.answerSets.filter { it.id eq answerSetId }.any() } ?:
            throw ErrorStatusException(404, "No AnswerSet with Id '$answerSetId' found.", ctx)

            val answerSet = dbSubmission.answerSets.filter { it.id eq answerSetId }.first()
            val verdictStatus = apiVerdictStatus.toDb()
            answerSet.status = verdictStatus

            AuditLogger.overrideVerdict(answerSet.toApi(), verdictStatus.toApi(), AuditLogSource.REST, ctx.sessionToken())

            SuccessStatus("Set status of AnswerSet '$answerSetId' to '${apiVerdictStatus.name}'")
        }
    }
}
