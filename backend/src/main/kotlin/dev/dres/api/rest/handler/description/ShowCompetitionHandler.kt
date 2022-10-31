package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.competition.ApiCompetitionDescription
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.competition.CompetitionDescription
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to show an existing [CompetitionDescription].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ShowCompetitionHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), GetRestHandler<ApiCompetitionDescription> {
    @OpenApi(
        summary = "Loads the detailed definition of a specific competition.",
        path = "/api/v1/competition/{competitionId}",
        pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
        tags = ["Competition"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiCompetitionDescription::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context)= this.store.transactional(true) {
       competitionFromContext(ctx).toApi()
    }
    override val route: String = "competition/{competitionId}"
}