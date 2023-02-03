package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.utilities.extensions.evaluationId
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.RunActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * REST handler to move to the specific task within an [Evaluation].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class SwitchTaskHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/task/switch/{idx}"

    @OpenApi(
        summary = "Moves to and selects the specified task. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/task/switch/{idx}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("idx", Int::class, "Index of the task to switch to.", required = true, allowEmptyValue = false)
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val idx = ctx.pathParamMap()["idx"]?.toIntOrNull() ?: throw ErrorStatusException(404, "Parameter 'idx' is missing!'", ctx)
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)
        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)
            try {
                evaluationManager.goTo(rac, idx)
                SuccessStatus("Task for evaluation $evaluationId was successfully moved to '${evaluationManager.currentTaskTemplate(rac).name}'.")
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, "Task for evaluation $evaluationId could not be changed because run is in the wrong state (state = ${evaluationManager.status}).", ctx)
            } catch (e: IndexOutOfBoundsException) {
                throw ErrorStatusException(404, "Task for evaluation $evaluationId could not be changed because index $idx is out of bounds for number of available tasks.", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}
