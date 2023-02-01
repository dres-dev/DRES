package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.template.task.TaskTemplate
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to list all [TaskTemplate]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListTasksHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<List<ApiTaskTemplate>> {

    override val route: String = "template/{templateId}/task/list"

    @OpenApi(
        summary = "Lists the task templates contained in a specific evaluation template.",
        path = "/api/v2/competition/{templateId}/task/list",
        pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.")],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTaskTemplate>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context) = this.store.transactional(true) {
        competitionFromContext(ctx).tasks.asSequence().map { it.toApi() }.toList()
    }
}