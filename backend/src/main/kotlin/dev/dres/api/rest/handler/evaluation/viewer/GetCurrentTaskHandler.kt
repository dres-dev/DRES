package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractEvaluationViewerHandler] that returns the currently active [DbTaskTemplate].
 *
 * If a [DbTask] is being executed, the method returns the [DbTaskTemplate] for that [DbTask].
 * Otherwise, the selected [DbTaskTemplate] is returned (active task vs. task template navigation).
 *
 * Only eligible for [InteractiveRunManager]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class GetCurrentTaskHandler : AbstractEvaluationViewerHandler(), GetRestHandler<ApiTaskTemplateInfo> {

    override val route = "evaluation/{evaluationId}/task"

    @OpenApi(
        summary = "Returns the information for the currently active task template (i.e., the one that is currently selected).",
        path = "/api/v2/evaluation/{evaluationId}/task",
        operationId = OpenApiOperation.AUTO_GENERATE,
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
        val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }
        val rac = ctx.runActionContext()
        return ApiTaskTemplateInfo(manager.currentTaskTemplate(rac))
    }
}
