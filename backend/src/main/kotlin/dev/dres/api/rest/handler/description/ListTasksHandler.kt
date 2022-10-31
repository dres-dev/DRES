package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.tasks.ApiTaskDescription
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.competition.task.TaskDescription
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.flatMapDistinct

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to list all [TaskDescription]s.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ListTasksHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<List<ApiTaskDescription>> {

    override val route: String = "competition/{competitionId}/task/list"

    @OpenApi(
        summary = "Lists the tasks of a specific competition.",
        path = "/api/v1/competition/{competitionId}/task/list",
        pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
        tags = ["Competition"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTaskDescription>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )

    override fun doGet(ctx: Context) = this.store.transactional(true) {
        competitionFromContext(ctx).taskGroups.flatMapDistinct { it.tasks }.asSequence().map { it.toApi() }.toList()
    }
}