package dev.dres.api.rest.handler.evaluation.team

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.template.team.DbTeam
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import kotlinx.dnq.query.FilteringContext.eq

class UpdateTeamHandler(private val store: TransientEntityStore) : PatchRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    override val apiVersion = "v2"

    override val route = "template/team/{teamId}"

    @OpenApi(
        summary = "Creates a new team.",
        path = "/api/v2/template/team/{teamId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiTeam::class)]),
        pathParams = [OpenApiParam("teamId", String::class, "The team ID.", required = true, allowEmptyValue = false)],
        tags = ["Template", "Team"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {

        val teamId = ctx.pathParam("teamId")

        val apiTeam = try {
            ctx.bodyAsClass<ApiTeam>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        return this.store.transactional(readonly = false) {
            val team = DbTeam.filter { it.id eq teamId }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown team $teamId.", ctx)

            apiTeam.name?.let {
                team.name = it
            }

            apiTeam.color?.let {
                team.color = it
            }

            apiTeam.logoStream()?.let {
                team.logo = it
            }

            if(apiTeam.users.isNotEmpty()) {
                team.users.clear()
                team.users.addAll(DbUser.query(DbUser::id.containsIn(*apiTeam.users.map { it.id }.toTypedArray())))
            }

            SuccessStatus("Team $teamId updated")

        }

    }
}