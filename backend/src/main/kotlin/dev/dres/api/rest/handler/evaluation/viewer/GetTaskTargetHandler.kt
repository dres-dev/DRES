package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.competition.tasks.ApiTargetContent
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.TaskStatus
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import java.io.FileNotFoundException
import java.io.IOException

/**
 *
 */
class GetTaskTargetHandler(store: TransientEntityStore, private val config: Config) : AbstractEvaluationViewerHandler(store), GetRestHandler<ApiTargetContent> {

    override val route = "evaluation/{evaluationId}/target/{taskId}"

    @OpenApi(
        summary = "Returns the task target for the current task run (i.e. the one that is currently selected).",
        path = "/api/v2/evaluation/{evaluationId}/target/{taskId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("taskId", String::class, "The task ID.", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiTargetContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiTargetContent {
        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(400, "Parameter 'taskId' not specified.", ctx)
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have an evaluation state.'", ctx)
        return this.store.transactional (true) {
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }
            val rac = RunActionContext.runActionContext(ctx, manager)

            /* Test for correct state. */
            var task = manager.currentTask(rac)
            if (task == null) {
                task = manager.taskForId(rac, taskId) ?: throw ErrorStatusException(404, "Task with specified ID $taskId does not exist.", ctx)
            }
            if (task.status != TaskStatus.ENDED) {
                throw ErrorStatusException(400, "Query target can only be loaded if task has just ended.", ctx)
            }

            try {
                ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
                task.template.toTaskTarget(config)
            } catch (e: FileNotFoundException) {
                throw ErrorStatusException(404, "Query object cache file not found!", ctx)
            } catch (ioe: IOException) {
                throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
            }
        }
    }
}
