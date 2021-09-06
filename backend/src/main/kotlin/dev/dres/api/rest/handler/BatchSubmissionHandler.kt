package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DaoIndexer
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.media.MediaItemSegmentList
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.batch.*
import dev.dres.run.InteractiveRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.utilities.TimeUtil
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class BatchSubmissionHandler(internal val collections: DAO<MediaCollection>, internal val itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>, internal val segmentIndex: DaoIndexer<MediaItemSegmentList, UID>) : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val apiVersion = "v1"
    override val permittedRoles: Set<Role> = setOf(RestApiRole.PARTICIPANT)

    internal fun userId(ctx: Context): UID = AccessManager.getUserIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)

    fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(404, "Parameter 'runId' is missing!'", ctx)
    }.UID()

    protected fun getInteractiveManager(userId: UID, runId: UID): InteractiveRunManager?
        = AccessManager.getRunManagerForUser(userId).filterIsInstance<InteractiveRunManager>().find { it.id == runId }

    protected fun getNonInteractiveManager(userId: UID, runId: UID): NonInteractiveRunManager?
            = AccessManager.getRunManagerForUser(userId).filterIsInstance<NonInteractiveRunManager>().find { it.id == runId }
}

data class JsonBatchSubmission(val batches : List<JsonTaskResultBatch>)

data class JsonTaskResultBatch(val taskName: String, val resultName: String, val results: List<JsonTaskResult>)

data class JsonTaskResult(val item: String, val segment: Int?)

class JsonBatchSubmissionHandler(collections: DAO<MediaCollection>, itemIndex: DaoIndexer<MediaItem, Pair<UID, String>>, segmentIndex: DaoIndexer<MediaItemSegmentList, UID>) : BatchSubmissionHandler(collections, itemIndex, segmentIndex) {

    override val route: String = "batchSubmit/:runId/json"

    @OpenApi(summary = "Endpoint to accept batch submissions in JSON format",
        path = "/api/v1/batchSubmit/:runId/json",
        method = HttpMethod.POST,
        pathParams = [OpenApiParam("runId", UID::class, "Competition Run ID")],
        requestBody = OpenApiRequestBody([OpenApiContent(JsonBatchSubmission::class)]),
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

        val jsonBatch = try{
            ctx.body<JsonBatchSubmission>()
        } catch (e: Exception) {
            throw ErrorStatusException(400, "Error parsing json batch", ctx)
        }

        val team = runManager.description.teams.find {
            it.users.contains(userId)
        }?.uid ?: throw ErrorStatusException(404, "No team for user '$userId' could not be found.", ctx)

        val resultBatches = jsonBatch.batches.mapNotNull { batch ->
            val task = runManager.tasks(rac).find { it.description.name == batch.taskName } ?: return@mapNotNull null
            val mediaCollectionId = task.description.mediaCollectionId
            val results = batch.results.map { result ->
                val mediaItem = this.itemIndex[mediaCollectionId to result.item].first() //TODO deal with invalid name
                return@map if (mediaItem is MediaItem.VideoItem && result.segment != null) {
                    val segmentList = segmentIndex[mediaItem.id].first()
                    val time = TimeUtil.shotToTime(result.segment.toString(), segmentList)!!
                    TemporalBatchElement(mediaItem, time.first, time.second)
                } else {
                    ItemBatchElement(mediaItem)
                }
            }

            if (results.all { it is TemporalBatchElement }) {
                @Suppress("UNCHECKED_CAST")
                (BaseResultBatch(mediaCollectionId, batch.resultName, team, results as List<TemporalBatchElement>))
            } else {
                BaseResultBatch(mediaCollectionId, batch.resultName, team, results)
            }

        }

        val submissionBatch = if (resultBatches.all { it.results.first() is TemporalBatchElement }) {
            @Suppress("UNCHECKED_CAST")
            (TemporalSubmissionBatch(team, userId, UID(), resultBatches as List<BaseResultBatch<TemporalBatchElement>>))
        } else {
            BaseSubmissionBatch(team, userId, UID(), resultBatches as List<BaseResultBatch<BaseResultBatchElement>>)
        }

        runManager.addSubmissionBatch(submissionBatch)

        return SuccessStatus("Submission batch received")

    }



}