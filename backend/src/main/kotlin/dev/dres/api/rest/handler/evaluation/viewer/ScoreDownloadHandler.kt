package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.AbstractScoreRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ScoreDownloadHandler(store: TransientEntityStore) : AbstractScoreRestHandler(), GetRestHandler<String> {

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
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): String {

        val runId = ctx.pathParamMap().getOrElse("runId") { throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx) }.UID()
        val run = this.runs[runId] ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        /* Update response header. */
        ctx.contentType("text/csv")
        ctx.header("Content-Disposition", "attachment; filename=\"scores-${runId.string}.csv\"")

        if (run is InteractiveAsynchronousEvaluation) {

            return "startTime,task,group,team,score\n" + run.tasks.filter { it.started != null }.sortedBy { it.started }
                .flatMap { task ->
                    task.scorer.scores().filter { it.first == task.teamId }
                        .map { "${task.started},\"${task.description.name}\",\"${task.description.taskGroup.name}\",\"${run.description.teams.find { t -> t.uid == it.first }?.name ?: "???"}\",${it.third}" }
                }.joinToString(separator = "\n")

        }

        return "startTime,task,group,team,score\n" + run.tasks.filter { it.started != null }.sortedBy { it.started }
            .flatMap { task ->
                task.scorer.scores().map { "${task.started},\"${task.description.name}\",\"${task.description.taskGroup.name}\",\"${run.description.teams.find { t -> t.uid == it.first }?.name ?: "???"}\",${it.third}" }
            }.joinToString(separator = "\n")
    }

}