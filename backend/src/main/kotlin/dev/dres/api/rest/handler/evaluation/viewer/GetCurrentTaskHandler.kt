package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.Task
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractEvaluationViewerHandler] that returns the currently active [TaskTemplate].
 *
 * If a [Task] is being executed, the method returns the [TaskTemplate] for that [Task].
 * Otherwise, the selected [TaskTemplate] is returned (active task vs. task template navigation).
 *
 * Only eligible for [InteractiveRunManager]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class GetCurrentTaskHandler(store: TransientEntityStore): AbstractEvaluationViewerHandler(store), GetRestHandler<ApiTaskTemplateInfo> {

    override val route = "evaluation/{evaluationId}/task"

    @OpenApi(
        summary = "Returns the information for the currently active task template (i.e., the one that is currently selected).",
        path = "/api/v1/evaluation/{evaluationId}/task",
        tags = ["Evaluation"],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTaskTemplateInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTaskTemplateInfo {
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have a current task.'", ctx)
        return this.store.transactional (true) {
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access denied.", ctx)
            }
            val rac = RunActionContext.runActionContext(ctx, manager)
            ApiTaskTemplateInfo(manager.currentTaskTemplate(rac))
        }
    }
}