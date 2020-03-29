package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import dres.data.model.competition.Task
import dres.data.model.competition.Team
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import java.lang.IllegalArgumentException

abstract class CompetitionHandler(protected val competitions: DAO<Competition>) : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    private fun competitionId(ctx: Context): Long =
            ctx.pathParamMap().getOrElse("competitionId") {
                throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'")
            }.toLong()

    private fun competitionById(id: Long): Competition =
            competitions[id] ?: throw ErrorStatusException(404, "Competition with ID $id not found.'")

    protected fun competitionFromContext(ctx: Context): Competition = competitionById(competitionId(ctx))

}

data class CompetitionOverview(val id: Long, val name: String, val description: String, val taskCount: Int, val teamCount: Int) {
    companion object {
        fun of(competition: Competition): CompetitionOverview = CompetitionOverview(competition.id, competition.name, competition.description
                ?: "", competition.tasks.size, competition.teams.size)
    }
}

class ListCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<CompetitionOverview>> {

    @OpenApi(
            summary = "Lists an overview of all available competitions with basic information about their content.",
            path = "/api/competition",
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<CompetitionOverview>::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context)  = competitions.map { CompetitionOverview.of(it) }

    override val route: String = "competition"
}

class GetCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<Competition> {

    @OpenApi(
            summary = "Loads the detailed definition of a specific competition.",
            path = "/api/competition/:competitionId",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Competition::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx)

    override val route: String = "competition/:competitionId"
}

class ListTeamHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<Team>> {

    override val route: String = "competition/:competitionId/team"

    @OpenApi(
            summary = "Lists the Teams of a specific competition.",
            path = "/api/competition/:competitionId/team",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<Team>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context) = competitionFromContext(ctx).teams

}

class ListTaskHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), GetRestHandler<List<Task>> {

    override val route: String = "competition/:competitionId/task"

    @OpenApi(
            summary = "Lists the Tasks of a specific competition.",
            path = "/api/competition/:competitionId/task",
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<Task>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )

    override fun doGet(ctx: Context) = competitionFromContext(ctx).tasks

}

class CreateCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Creates a new competition.",
            path = "/api/competition", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(CompetitionOverview::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val createRequest = try {
            ctx.bodyAsClass(CompetitionOverview::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")

        }

        val competition = Competition(-1L, createRequest.name, createRequest.description, mutableListOf(), mutableListOf())
        val competitionId = this.competitions.append(competition)
        return SuccessStatus("Competition with ID $competitionId was created.")
    }

    override val route: String = "competition"
}


class DeleteCompetitionHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), DeleteRestHandler<SuccessStatus> {
    @OpenApi(
            summary = "Deletes the competition with the given competition ID.",
            path = "/api/competition/:competitionId", method = HttpMethod.DELETE,
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val competitionToDelete = competitionFromContext(ctx)

        val competition = this.competitions.delete(competitionToDelete.id)
        return SuccessStatus("Competition with ID ${competitionToDelete.id} was deleted.")
    }

    override val route: String = "competition/:competitionId"
}

class AddTeamHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {

    override val route: String = "competition/:competitionId/team"

    @OpenApi(
            summary = "Adds a Team to a specific competition.",
            path = "/api/competition/:competitionId/team",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            requestBody = OpenApiRequestBody([OpenApiContent(Team::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val createRequest = try {
            ctx.bodyAsClass(Team::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competition = competitionFromContext(ctx)

        try {
            competition.addTeam(createRequest)
        }catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message!!)
        }

        competitions.update(competition)

        return SuccessStatus("Team added to competition")

    }
}

class AddTaskHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), PostRestHandler<SuccessStatus> {

    override val route: String = "competition/:competitionId/task"

    @OpenApi(
            summary = "Adds a Task to a specific competition.",
            path = "/api/competition/:competitionId/task",
            method = HttpMethod.POST,
            pathParams = [OpenApiParam("competitionId", Long::class, "Competition ID")],
            requestBody = OpenApiRequestBody([OpenApiContent(Task::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val createRequest = try {
            ctx.bodyAsClass(Task::class.java)
        }catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val competition = competitionFromContext(ctx)

        try {
            competition.addTask(createRequest)
        }catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message!!)
        }

        competitions.update(competition)

        return SuccessStatus("Task added to competition")

    }
}

class DeleteTaskHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), DeleteRestHandler<SuccessStatus> {

    override val route: String = "competition/:competitionId/task/:taskId"

    @OpenApi(
            summary = "Deletes a specific Task from a specific competition.",
            path = "/api/competition/:competitionId/task/:taskId",
            method = HttpMethod.DELETE,
            pathParams = [
                OpenApiParam("competitionId", Long::class, "Competition ID"),
                OpenApiParam("taskId", Long::class, "Task ID")
            ],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {

        val competition = competitionFromContext(ctx)

        val taskId = ctx.pathParamMap().getOrElse("taskId") {
            throw ErrorStatusException(404, "Parameter 'taskId' is missing!'")
        }.toLong()

        val task = competition.tasks.find { it.id == taskId } ?:
                throw ErrorStatusException(404, "No task with id '$taskId' in competition")

        competition.tasks.remove(task)

        competitions.update(competition)

        return SuccessStatus("Task '$taskId' deleted from competition")

    }

}

class DeleteTeamHandler(competitions: DAO<Competition>) : CompetitionHandler(competitions), DeleteRestHandler<SuccessStatus> {

    override val route: String = "competition/:competitionId/team/:teamId"

    @OpenApi(
            summary = "Deletes a specific Team from a specific competition.",
            path = "/api/competition/:competitionId/team/:teamId",
            method = HttpMethod.DELETE,
            pathParams = [
                OpenApiParam("competitionId", Long::class, "Competition ID"),
                OpenApiParam("teamId", Long::class, "Team ID")
            ],
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {

        val competition = competitionFromContext(ctx)

        val teamId = ctx.pathParamMap().getOrElse("teamId") {
            throw ErrorStatusException(404, "Parameter 'teamId' is missing!'")
        }.toLong()

        val team = competition.teams.find { it.id == teamId } ?:
        throw ErrorStatusException(404, "No team with id '$teamId' in competition")

        competition.teams.remove(team)

        competitions.update(competition)

        return SuccessStatus("Team '$team' deleted from competition")

    }

}