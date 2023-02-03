package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 * [PostRestHandler] to start the current task within an [DbEvaluation].

 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class StartTaskHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {

    override val route: String = "evaluation/admin/{evaluationId}/task/start"

    /** The [StartTaskHandler] can be used by [ApiRole.ADMIN] and [ApiRole.PARTICIPANT] (in case of asynchronous evaluations). */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN, ApiRole.PARTICIPANT)

    @OpenApi(
        summary = "Starts the currently active task as a new task run. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/task/start",
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evalation ID.", required = true, allowEmptyValue = false)],
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

        /* Important: Check that user can actually change this manager. */
        synchronousAdminCheck(evaluationManager, ctx)

        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)
            try {
                evaluationManager.startTask(rac)
                AuditLogger.taskStart(evaluationManager.id, evaluationManager.currentTask(rac)!!.id, evaluationManager.currentTaskTemplate(rac), DbAuditLogSource.REST, ctx.sessionToken())
                SuccessStatus("Task '${evaluationManager.currentTaskTemplate(rac).name}' for evaluation $evaluationId was successfully started.")
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, e.message ?: "", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}