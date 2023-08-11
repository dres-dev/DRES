package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.isAdmin
import dev.dres.utilities.extensions.isParticipant
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

class ViewerReadyHandler(store: TransientEntityStore) : AbstractEvaluationViewerHandler(store), GetRestHandler<SuccessStatus> {

    override val route = "evaluation/{evaluationId}/hint/{taskId}/ready"

    @OpenApi(
        summary = "Signals that a viewer is ready to show the hints for a particular task.",
        path = "/api/v2/evaluation/{evaluationId}/hint/{taskId}/ready",
        tags = ["Evaluation"],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam("taskId", String::class, "The task ID.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): SuccessStatus {

        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(400, "Parameter 'taskId' not specified.", ctx)
        val rac = ctx.runActionContext()

        return this.store.transactional(true) {
            val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }

            if(ctx.isParticipant() || ctx.isAdmin()) {
                manager.viewerReady(
                    taskId, rac, ViewerInfo(
                        ctx.sessionToken()!!,
                        ctx.ip()
                    )
                )
            }


            SuccessStatus("ready received")
        }


    }

}