package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.handler.preview.AbstractPreviewHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.submission.RunResult
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.Config
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.Submission
import dev.dres.run.InteractiveRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import java.util.*

/**
 * An [GetRestHandler] used to process batched [Submission]s.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class BatchSubmissionHandler(private val store: TransientEntityStore, private val config: Config) : PostRestHandler<SuccessStatus>, AccessManagedRestHandler {
    /** [BatchSubmissionHandler] requires [ApiRole.PARTICIPANT]. */
    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    /** All [BatchSubmissionHandler]s are part of the v1 API. */
    override val apiVersion = "v1"

    override val route: String = "submit/{evaluationId}"

    @OpenApi(summary = "Endpoint to accept batch submissions in JSON format",
        path = "/api/v1/submit/{evaluationId}",
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.")],
        requestBody = OpenApiRequestBody([OpenApiContent(RunResult::class)]),
        tags = ["Batch Submission"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ])
    override fun doPost(ctx: Context): SuccessStatus {
        /* Try to parse message. */
        val runResult = try {
            ctx.bodyAsClass<RunResult>()
        } catch (e: Exception) {
            throw ErrorStatusException(400, "Error parsing JSON body", ctx)
        }

        /* Obtain basic information required for submission processing. */
        val userId = AccessManager.userIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
        val runManager = this.getEligibleRunManager(userId, ctx)
        val time = System.currentTimeMillis()
        this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, runManager)
            val submission = toSubmission(userId, runManager, runResult, time, ctx)
            runManager.postSubmission(rac, submission)
        }
        return  SuccessStatus("Submission received.")
    }

    /**
     * Converts the user request tu a [Submission].
     *
     * Creates the associated database entry. Requires an ongoing transaction.
     *
     * @param userId The [UserId] of the user who triggered the [Submission].
     * @param runManager The [InteractiveRunManager]
     * @param submission The submitted [RunResult]s.
     * @param submissionTime Time of the submission.
     * @param ctx The HTTP [Context]
     */
    private fun toSubmission(userId: UserId, runManager: NonInteractiveRunManager, submission: RunResult, submissionTime: Long, ctx: Context): Submission {
        /* Find team that the user belongs to. */
        val user = User.query(User::id eq userId).firstOrNull()
            ?: throw ErrorStatusException(404, "No user with ID '$userId' could be found.", ctx)
        val team = runManager.template.teams.filter { it.users.contains(user) }.firstOrNull()
            ?: throw ErrorStatusException(404, "No team for user '$userId' could not be found.", ctx)

        /* Create new submission. */
        val new = Submission.new {
            this.id = UUID.randomUUID().toString()
            this.user = user
            this.team = team
            this.timestamp = submissionTime
        }

        /* Process submitted results. */
        val resultBatches = submission.tasks.mapNotNull { taskResult ->
            /*val task = runManager.tasks(rac).find { it.template.name == taskResult.task } ?: return@mapNotNull null
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
            }*/
        }

        return new
    }

    /**
     * Returns the [NonInteractiveRunManager] that is eligible for the given [Context]
     */
    private fun getEligibleRunManager(userId: UserId, ctx: Context): NonInteractiveRunManager {
        val evaluationId = ctx.evaluationId()
        return AccessManager.getRunManagerForUser(userId).filterIsInstance<NonInteractiveRunManager>().find { it.id == evaluationId }
            ?: throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", ctx)
    }
}
