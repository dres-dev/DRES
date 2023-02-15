package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.audit.DbAuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * [PostRestHandler] to abort the current task within an [DbEvaluation].

 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class StopTaskHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/task/abort"

    @OpenApi(
        summary = "Aborts the currently running task run. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/task/abort",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)
            try {
                val task = evaluationManager.currentTaskTemplate(rac)
                evaluationManager.abortTask(rac)
                DbAuditLogger.taskEnd(evaluationManager.id, task.id, DbAuditLogSource.REST, ctx.sessionToken())
                SuccessStatus("Task '${evaluationManager.currentTaskTemplate(rac).name}' for evaluation $evaluationId was successfully aborted.")
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, "Task '${evaluationManager.currentTaskTemplate(rac).name}' for evaluation $evaluationId could not be aborted because run is in the wrong state (state = ${evaluationManager.status}).", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}
