package dev.dres.api.rest.handler.evaluation.team

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.template.team.ApiTeam
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.template.team.DbTeam
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

class ListAllTeamsHandler(private val store: TransientEntityStore) : GetRestHandler<List<ApiTeam>>, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    override val apiVersion = "v2"

    override val route = "template/team/list"

    @OpenApi(
        summary = "Lists all the teams across all evaluations.",
        path = "/api/v2/template/team/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Template", "Team"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTeam>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiTeam> = this.store.transactional(true) {
        DbTeam.all().asSequence().map { it.toApi() }.toList()
    }


}
