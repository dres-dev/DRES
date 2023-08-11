package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PatchRestHandler] handler to adjust an ongoing [DbTask]'s duration.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class AdjustDurationHandler : AbstractEvaluationAdminHandler(),
    PatchRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/adjust/{duration}"

    @OpenApi(
        summary = "Adjusts the duration of a running task. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/adjust/{duration}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.PATCH],
        pathParams = [
            OpenApiParam(
                "evaluationId",
                String::class,
                "The evaluation ID.",
                required = true,
                allowEmptyValue = false
            ),
            OpenApiParam(
                "duration",
                Int::class,
                "Duration to add. Can be negative.",
                required = true,
                allowEmptyValue = false
            )
        ],
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
        val duration = ctx.pathParamMap()["duration"]?.toIntOrNull() ?: throw ErrorStatusException(
            404,
            "Parameter 'duration' is missing!'",
            ctx
        )
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(
            404,
            "Evaluation $evaluationId not found",
            ctx
        )
        val rac = ctx.runActionContext()

        return try {
            try {
                evaluationManager.adjustDuration(rac, duration)
                AuditLogger.taskModified(
                    evaluationManager.id,
                    evaluationManager.currentTaskTemplate(rac).id!!,
                    "Task duration adjusted by ${duration}s.",
                    AuditLogSource.REST,
                    ctx.sessionToken()
                )
                SuccessStatus("Duration for run $evaluationId was successfully adjusted.")
            } catch (e: IllegalArgumentException) {
                evaluationManager.abortTask(rac)
                SuccessStatus("Successfully stopped task since the duration was below zero after adjusting.")
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(
                400,
                "Duration for run $evaluationId could not be adjusted because it is in the wrong state (state = ${evaluationManager.status}).",
                ctx
            )
        }


    }
}
