package dev.dres.api.rest.handler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.dbo.DAO
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.utilities.extensions.UID
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

/**
 * A [AccessManagedRestHandler] implementation that provides certain data structures as downloadable files.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
sealed class DownloadHandler : AccessManagedRestHandler {

    /** The version of the API this [DownloadHandler] belongs to. */
    override val apiVersion = "v1"

    /** The roles permitted to access the [DownloadHandler]. */
    override val permittedRoles: Set<RouteRole> = setOf(RestApiRole.ADMIN, RestApiRole.PARTICIPANT)

    /**
     * REST handler to download the competition run information.
     */
    class CompetitionRun(private val runs: DAO<Competition>) : DownloadHandler(), GetRestHandler<String> {

        /** The route of this [DownloadHandler.CompetitionRun]. */
        override val route = "download/run/{runId}"

        @OpenApi(
            summary = "Provides a JSON download of the entire competition run structure.",
            path = "/api/v1/download/run/{runId}",
            tags = ["Download"],
            pathParams = [
                OpenApiParam("runId", String::class, "Competition run ID")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(String::class, type = "application/json")]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
        )
        override fun doGet(ctx: Context): String {
            /* Obtain run id and run. */
            val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
            val run = this.runs[runId] ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

            /* Set header for download. */
            ctx.header("Content-Disposition", "attachment; filename=\"run-${runId.string}.json\"")

            /* Return value. */
            val mapper = jacksonObjectMapper()
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(run)
        }
    }

    /**
     * REST handler to download the competition run scores description information.
     */
    class CompetitionRunScore(private val runs: DAO<Competition>) : AbstractScoreRestHandler(), GetRestHandler<String> {

        override val route = "download/run/{runId}/scores"

        @OpenApi(
            summary = "Provides a CSV download with the scores for a given competition run.",
            path = "/api/v1/download/run/{runId}/scores",
            tags = ["Download"],
            pathParams = [
                OpenApiParam("runId", String::class, "Competition run ID")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(String::class, type = "text/csv")]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
        )
        override fun doGet(ctx: Context): String {

            val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
            val run = this.runs[runId] ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

            /* Update response header. */
            ctx.contentType("text/csv")
            ctx.header("Content-Disposition", "attachment; filename=\"scores-${runId.string}.csv\"")

            return "task,group,team,resultName,score\n" + run.tasks.filter { it.started != null}.sortedBy { it.started }.flatMap { task ->
                task.scorer.scores().map { "${task.description.name},${task.description.taskGroup.name},${run.description.teams.find { t -> t.uid == it.first }?.name ?: "???"},${it.second ?: "n/a"},${it.third}" }
            }.joinToString(separator = "\n")
        }

    }

    /**
     * REST handler to download the competition description information.
     */
    class CompetitionDesc(private val competitions: DAO<CompetitionDescription>) : DownloadHandler(), GetRestHandler<String> {

        /** The route of this [DownloadHandler.CompetitionRun]. */
        override val route = "download/competition/{competitionId}"

        @OpenApi(
            summary = "Provides a JSON download of the entire competition description structure.",
            path = "/api/v1/download/competition/{competitionId}",
            tags = ["Download"],
            pathParams = [
                OpenApiParam("competitionId", String::class, "Competition ID")
            ],
            responses = [
                OpenApiResponse("200", [OpenApiContent(String::class, type = "application/json")]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
        )
        override fun doGet(ctx: Context): String {
            /* Obtain run id and run. */
            val competitionId = ctx.pathParamMap().getOrElse("competitionId") { throw ErrorStatusException(400, "Parameter 'competitionId' is missing!'", ctx) }.UID()
            val competition = this.competitions[competitionId] ?: throw ErrorStatusException(404, "Competition $competitionId not found", ctx)

            /* Set header for download. */
            ctx.header("Content-Disposition", "attachment; filename=\"competition-${competitionId.string}.json")

            /* Return value. */
            val mapper = jacksonObjectMapper()
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(competition)
        }
    }
}
