package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.competition.CompetitionDescription
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to delete an existing [CompetitionDescription].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class DeleteCompetitionHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), DeleteRestHandler<SuccessStatus> {
    override val route: String = "competition/{competitionId}"

    @OpenApi(
        summary = "Deletes the competition description with the given competition ID.",
        path = "/api/v1/competition/{competitionId}",
        methods = [HttpMethod.DELETE],
        pathParams = [OpenApiParam("competitionId", String::class, "Competition ID")],
        tags = ["Competition"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus = this.store.transactional {
        val competitionToDelete = competitionFromContext(ctx)
        competitionToDelete.delete()
        SuccessStatus("Competition with ID ${competitionToDelete.id} was deleted successfully.")
    }
}

