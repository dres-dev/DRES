import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluation.viewer.AbstractEvaluationViewerHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.isParticipant
import dev.dres.api.rest.types.competition.tasks.ApiHintContent
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.Task
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import java.io.FileNotFoundException
import java.io.IOException

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
class GetTaskHintHandler(store: TransientEntityStore, private val config: Config) : AbstractEvaluationViewerHandler(store), GetRestHandler<ApiHintContent> {

    override val route = "run/{evaluationId}/hint/{taskId}"

    @OpenApi(
        summary = "Returns the task hint for the specified task.",
        path = "/api/v2/run/{evaluationId}/hint/{taskId}",
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam("taskId", String::class, "The task ID.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiHintContent::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiHintContent {
        val taskId = ctx.pathParamMap()["taskId"] ?: throw ErrorStatusException(400, "Parameter 'taskId' not specified.", ctx)
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have an evaluation state.'", ctx)
        return this.store.transactional (true) {
            if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
                throw ErrorStatusException(403, "Access Denied", ctx)
            }
            val rac = RunActionContext.runActionContext(ctx, manager)

            val currentTaskDescription = manager.currentTaskTemplate(rac)
            val task = if (currentTaskDescription.id == taskId) {
                currentTaskDescription
            } else {
                manager.taskForId(rac, taskId)?.template ?: throw ErrorStatusException(404, "Task with specified ID $taskId does not exist.", ctx)
            }

            try {
                ctx.header("Cache-Control", "public, max-age=300") //can be cached for 5 minutes
                task.toTaskHint(this.config)
            } catch (e: FileNotFoundException) {
                throw ErrorStatusException(404, "Query object cache file not found!", ctx)
            } catch (ioe: IOException) {
                throw ErrorStatusException(500, "Exception when reading query object cache file.", ctx)
            }
        }
    }
}