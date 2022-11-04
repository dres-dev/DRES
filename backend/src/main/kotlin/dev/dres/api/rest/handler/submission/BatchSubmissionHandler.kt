package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.submission.RunResult
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaItemSegmentList
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.batch.*
import dev.dres.run.InteractiveRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.utilities.TimeUtil
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import io.javalin.security.RouteRole

abstract class BatchSubmissionHandler(internal val collections: DAO<MediaCollection>, internal val itemIndex: DaoIndexer<MediaItem, Pair<EvaluationId, String>>, internal val segmentIndex: DaoIndexer<MediaItemSegmentList, EvaluationId>) : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val apiVersion = "v1"
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.PARTICIPANT)

    internal fun userId(ctx: Context): EvaluationId = AccessManager.userIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'", ctx)
    }.UID()

    protected fun getInteractiveManager(userId: EvaluationId, runId: EvaluationId): InteractiveRunManager?
        = AccessManager.getRunManagerForUser(userId).filterIsInstance<InteractiveRunManager>().find { it.id == runId }

    protected fun getNonInteractiveManager(userId: EvaluationId, runId: EvaluationId): NonInteractiveRunManager?
            = AccessManager.getRunManagerForUser(userId).filterIsInstance<NonInteractiveRunManager>().find { it.id == runId }
}

class JsonBatchSubmissionHandler(collections: DAO<MediaCollection>, itemIndex: DaoIndexer<MediaItem, Pair<EvaluationId, String>>, segmentIndex: DaoIndexer<MediaItemSegmentList, EvaluationId>) : BatchSubmissionHandler(collections, itemIndex, segmentIndex) {

    override val route: String = "submit/{runId}"

    @OpenApi(summary = "Endpoint to accept batch submissions in JSON format",
        path = "/api/v1/submit/{runId}",
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("runId", String::class, "Competition Run ID")],
        requestBody = OpenApiRequestBody([OpenApiContent(RunResult::class)]),
        tags = ["Batch Submission"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
        )
    override fun doPost(ctx: Context): SuccessStatus {

        val userId = userId(ctx)
        val runId = runId(ctx)

        val runManager = getNonInteractiveManager(userId, runId) ?: throw ErrorStatusException(404, "Run ${runId.string} not found", ctx)

        val rac = RunActionContext.runActionContext(ctx, runManager)

        val runResult = try{
            ctx.bodyAsClass<RunResult>()
        } catch (e: Exception) {
            throw ErrorStatusException(400, "Error parsing json batch", ctx)
        }

        val team = runManager.template.teams.find {
            it.users.contains(userId)
        }?.uid ?: throw ErrorStatusException(404, "No team for user '$userId' could not be found.", ctx)

        val resultBatches = runResult.tasks.mapNotNull { taskResult ->
            val task = runManager.tasks(rac).find { it.template.name == taskResult.task } ?: return@mapNotNull null
            val mediaCollectionId = task.template.mediaCollectionId
            val results = taskResult.results.map { result ->
                if (result.item != null) {
                    val mediaItem =
                        this.itemIndex[mediaCollectionId to result.item].first() //TODO deal with invalid name
                    return@map if (mediaItem is MediaItem.VideoItem && (result.startTimeCode != null || result.endTimeCode != null || result.index != null)) {

                        val time = if (result.index != null) {
                            val segmentList = segmentIndex[mediaItem.id].first()
                            TimeUtil.shotToTime(result.index.toString(), segmentList)!!
                        } else {
                            val start = if (result.startTimeCode != null) {
                                TemporalPoint.Timecode.timeCodeToMilliseconds(result.startTimeCode, mediaItem.fps)!! //FIXME error handling
                            } else {
                                TemporalPoint.Timecode.timeCodeToMilliseconds(result.endTimeCode!!, mediaItem.fps)!!
                            }
                            val end = if (result.endTimeCode != null) {
                                TemporalPoint.Timecode.timeCodeToMilliseconds(result.endTimeCode, mediaItem.fps)!!
                            } else {
                                start
                            }
                            start to end
                        }
                        TemporalBatchElement(mediaItem, time.first, time.second)
                    } else {
                        ItemBatchElement(mediaItem)
                    }
                } else { //TODO deal with text
                    TODO("text batch submissions not yet supported")
                }
            }

            if (results.all { it is TemporalBatchElement }) {
                @Suppress("UNCHECKED_CAST")
                (BaseResultBatch(mediaCollectionId, taskResult.resultName, team, results as List<TemporalBatchElement>))
            } else {
                BaseResultBatch(mediaCollectionId, taskResult.resultName, team, results)
            }

        }

        val submissionBatch = if (resultBatches.all { it.results.first() is TemporalBatchElement }) {
            @Suppress("UNCHECKED_CAST")
            (TemporalSubmissionBatch(team, userId, EvaluationId(), resultBatches as List<BaseResultBatch<TemporalBatchElement>>))
        } else {
            BaseSubmissionBatch(team, userId, EvaluationId(), resultBatches as List<BaseResultBatch<BaseResultBatchElement>>)
        }

        runManager.addSubmissionBatch(submissionBatch)

        return SuccessStatus("Submission batch received")

    }

}
