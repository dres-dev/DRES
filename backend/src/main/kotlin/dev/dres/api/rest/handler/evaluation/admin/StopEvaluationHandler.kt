package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * [PostRestHandler] to stop an ongoing [Evaluation].

 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class StopEvaluationHandler : AbstractEvaluationAdminHandler(), PostRestHandler<SuccessStatus> {

    override val route: String = "evaluation/admin/{evaluationId}/terminate"

    @OpenApi(
        summary = "Terminates an evaluation. This is a method for administrators.",
        path = "/api/v2/evaluation/admin/{evaluationId}/terminate",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam(
            "evaluationId",
            String::class,
            "The evaluation ID.",
            required = true,
            allowEmptyValue = false
        )],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val evaluationManager =
            getManager(evaluationId) ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)


        val rac = ctx.runActionContext()
        try {
            evaluationManager.end(rac)
            AuditLogger.evaluationEnd(
                evaluationManager.id,
                AuditLogSource.REST,
                AccessManager.userIdForSession(ctx.sessionToken())!!,
                ctx.sessionToken()
            )
            return SuccessStatus("Evaluation $evaluationId was successfully stopped.")
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(
                400,
                "Evaluation $evaluationId could not be stopped because it is in the wrong state (state = ${evaluationManager.status}).",
                ctx
            )
        } catch (e: IllegalAccessError) {
            throw ErrorStatusException(403, e.message!!, ctx)
        }

    }
}
