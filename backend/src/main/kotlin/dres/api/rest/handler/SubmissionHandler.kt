package dres.api.rest.handler


import dres.api.rest.AccessManager
import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.competition.Task
import dres.data.model.competition.TaskType
import dres.data.model.run.AvsSubmission
import dres.data.model.run.KisSubmission
import dres.data.model.run.Submission
import dres.run.RunExecutor
import dres.run.RunManager
import dres.run.RunManagerStatus
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

class SubmissionHandler : GetRestHandler<SuccessStatus>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.PARTICIPANT)
    override val route = "submit"

    private fun userId(ctx: Context): Long = AccessManager.getUserIdforSession(ctx.req.session.id)!!

    private fun getRelevantManagers(ctx: Context): List<RunManager> { //TODO there needs to be a more efficient way to do this
        val userId = userId(ctx)
        return RunExecutor.managers().filter { it.competition.teams.any { it.users.contains(userId) } }
    }

    private fun getActiveCompetition(ctx: Context): RunManager {
        val managers = getRelevantManagers(ctx).filter { it.status == RunManagerStatus.RUNNING_TASK }

        if (managers.isEmpty()) {
            throw ErrorStatusException(404, "No Competition with active tasks")
        }

        if (managers.size > 1) {
            throw ErrorStatusException(409, "More than one possible Competition found: ${managers.map { it.competition.name }.joinToString()}")
        }

        return managers.first()
    }

    private fun toSubmission(ctx: Context, currentTask: Task, submissionTime: Long): Submission {

        val map = ctx.pathParamMap()

        val team = map.getOrElse("team") { //TODO replace with team from session
            throw ErrorStatusException(404, "Parameter 'team' is missing!'")
        }.toInt()

        val video = map.getOrElse("video") {
            throw ErrorStatusException(404, "Parameter 'video' is missing!'")
        }

        if (!map.containsKey("frame") && !map.containsKey("shot") && !map.containsKey("timecode")) {
            throw ErrorStatusException(404, "Neither Parameter 'frame', 'shot', nor 'timecode' os present")
        }

        //TODO map frame and shot to time
        val time = map.getOrDefault("timecode", "0").toLong()

        return when(currentTask.description.taskType) {
            TaskType.KIS_VISUAL -> {
                KisSubmission(team, submissionTime, video, time, time)
            }
            TaskType.KIS_TEXTUAL -> {
                KisSubmission(team, submissionTime, video, time, time)
            }
            TaskType.AVS -> {
                AvsSubmission(team, submissionTime, video)
            }
        }

    }

    @OpenApi(summary = "Endpoint to accept submissions",
            path = "/submit",
            queryParams = [
                OpenApiParam("team", Int::class, "Team number"),
                OpenApiParam("video", String::class, "Video ID for VBS Submissions"),
                OpenApiParam("image", String::class, "Image ID for LSC Submissions"),
                OpenApiParam("frame", Int::class, "Frame number for VBS Submissions"),
                OpenApiParam("shot", Int::class, "Shot number for VBS Submissions"),
                OpenApiParam("timecode", String::class, "Timecode for VBS Submissions")
            ],
            tags = ["Submission"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): SuccessStatus {
        val time = System.currentTimeMillis()
        val competition = getActiveCompetition(ctx)
        competition.postSubmission(toSubmission(ctx, competition.currentTask!!, time))
        return SuccessStatus("")
    }

}