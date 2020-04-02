package dres.api.rest.handler


import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.run.Submission
import dres.run.RunManager
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

class SubmissionHandler : GetRestHandler<SuccessStatus>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.PARTICIPANT)
    override val route = "submit"

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
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )


    private fun toSubmission(ctx: Context): Submission {
        TODO("Not yet implemented")
    }

    private fun getActiveCompetition(ctx: Context): RunManager {
        TODO("Not yet implemented")
    }

    override fun doGet(ctx: Context): SuccessStatus {
        getActiveCompetition(ctx).postSubmission(toSubmission(ctx))
        return SuccessStatus("")
    }

}