package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.DbTaskStatus
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 * REST handler to move to the next task in a [InteractiveSynchronousEvaluation].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class NextTaskHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/task/next"

    /** The [NextTaskHandler] can be used by [ApiRole.ADMIN] and [ApiRole.PARTICIPANT] (in case of asynchronous evaluations). */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN, ApiRole.PARTICIPANT)

    @OpenApi(
        summary = "Moves to and selects the next task within the evaluation. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/task/next",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus{
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)

        /* Important: Check that user can actually change this manager. */
        synchronousAdminCheck(evaluationManager, ctx)

        return this.store.transactional(false) {
            val rac = ctx.runActionContext()
            if (evaluationManager is InteractiveAsynchronousRunManager
                && !AccessManager.rolesOfSession(ctx.sessionToken()).contains(ApiRole.ADMIN)
                && evaluationManager.currentTask(rac)?.status !in setOf(DbTaskStatus.ENDED, DbTaskStatus.IGNORED)) {
                throw ErrorStatusException(400, "Cannot advance to next task before current task is completed.", ctx)
            }

            try {
                if (evaluationManager.next(rac)) {
                    SuccessStatus("Task for evaluation $evaluationId was successfully moved to '${evaluationManager.currentTaskTemplate(rac).name}'.")
                } else {
                    throw ErrorStatusException(400, "Task for evaluation $evaluationId could not be changed because there are no tasks left.", ctx)
                }
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, "Task for evaluation $evaluationId could not be changed because evaluation is in the wrong state (state = ${evaluationManager.status}).", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}
