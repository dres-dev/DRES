package dev.dres.api.rest.handler.download

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.run.RunActionContext
import dev.dres.utilities.extensions.eligibleManagerForId
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull

/**
 * An [AbstractDownloadHandler] that allows for download of score information.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ScoreDownloadHandler(store: TransientEntityStore) : AbstractDownloadHandler(store), GetRestHandler<String> {

    override val route = "download/evaluation/{evaluationId}/scores"

    @OpenApi(
        summary = "Provides a CSV download with the scores for a given evaluation.",
        path = "/api/v2/download/evaluation/{evaluationId}/scores",
        tags = ["Download"],
        pathParams = [
            OpenApiParam("runId", String::class, "The evaluation ID.", required = true)
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
        val manager = ctx.eligibleManagerForId()

        /* Update response header. */
        ctx.contentType("text/csv")
        ctx.header("Content-Disposition", "attachment; filename=\"scores-${manager.id}.csv\"")

        return this.store.transactional(false) {
            val rac = RunActionContext.runActionContext(ctx, manager)
            "startTime,task,group,team,score\n" + manager.tasks(rac).filter {
                it.started != null
            }.sortedBy {
                it.started
            }
            .flatMap { task ->
                task.scorer.scores().map { "${task.started},\"${task.template.name}\",\"${task.template.taskGroup.name}\",\"${manager.template.teams.filter { t -> t.id eq it.first }.firstOrNull()?.name ?: "???"}\",${it.third}" }
            }.joinToString(separator = "\n")
        }
    }

}