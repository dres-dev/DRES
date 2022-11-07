package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.sessionId
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
class StopEvaluationHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {

    override val route: String = "evaluation/admin/{evaluationId}/terminate"

    @OpenApi(
        summary = "Terminates an evaluation. This is a method for administrators.",
        path = "/api/v1/evaluation/admin/{runId}/terminate",
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)],
        tags = ["Competition Run Admin"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)

        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)
            try {
                evaluationManager.end(rac)
                AuditLogger.competitionEnd(evaluationManager.id, AuditLogSource.REST, ctx.sessionId())
                SuccessStatus("Evaluation $evaluationId was successfully stopped.")
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, "Evaluation $evaluationId could not be stopped because it is in the wrong state (state = ${evaluationManager.status}).", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}