package dev.dres.api.rest.handler.evaluation.team

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.template.team.ApiTeam
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
import kotlinx.dnq.query.addAll
import kotlinx.dnq.query.containsIn
import kotlinx.dnq.query.query

class CreateTeamHandler(private val store: TransientEntityStore) : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    override val apiVersion = "v2"

    override val route = "template/team"

    @OpenApi(
        summary = "Creates a new team.",
        path = "/api/v2/template/team",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiTeam::class)]),
        tags = ["Template", "Team"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val createRequest = try {
            ctx.bodyAsClass<ApiTeam>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        return this.store.transactional(readonly = false) {

            val team = DbTeam.new {//id is set automatically on create
                this.name = createRequest.name ?: throw ErrorStatusException(404, "Team name must be specified.", ctx)
                this.color = createRequest.color ?: throw ErrorStatusException(404, "Team color must be specified.", ctx)
            }

            /* Process logo data. */
            val logoData = createRequest.logoStream()
            if (logoData != null) {
                team.logo = logoData
            }

            team.users.addAll(DbUser.query(DbUser::id.containsIn(*createRequest.users.map { it.id }.toTypedArray())))

            return@transactional SuccessStatus("Team created")

        }

    }

}
