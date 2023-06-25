package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.evaluation.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.config.Config
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.run.NonInteractiveEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.submissions.AnswerType
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManager
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import kotlinx.dnq.query.firstOrNull
import org.slf4j.LoggerFactory

class SubmissionHandler(private val store: TransientEntityStore, private val config: Config) :
    PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    override val apiVersion = "v2"

    override val route = "submit/{evaluationId}"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @OpenApi(
        summary = "Endpoint to accept submissions",
        path = "/api/v2/submit/{evaluationId}",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiClientSubmission::class)]),
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("412", [OpenApiContent(ErrorStatus::class)])
        ],
        tags = ["Submission"]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        return this.store.transactional {

            val userId = AccessManager.userIdForSession(ctx.sessionToken()) ?: throw ErrorStatusException(
                401,
                "Authorization required.",
                ctx
            )
            val evaluationId = ctx.pathParam("evaluationId")
            val runManager =
                AccessManager.getRunManagerForUser(userId).find { it.id == evaluationId } ?: throw ErrorStatusException(
                    404,
                    "Evaluation with id '$evaluationId' not found.",
                    ctx
                )

            val rac = RunActionContext.runActionContext(ctx, runManager)

            val apiClientSubmission = try {
                ctx.bodyAsClass<ApiClientSubmission>()
            } catch (e: BadRequestResponse) {
                throw ErrorStatusException(400, "Invalid submission, cannot parse: ${e.message}", ctx)
            }

            val apiSubmission = try {
                transformClientSubmission(apiClientSubmission, runManager, rac)
            } catch (e: Exception) {
                throw ErrorStatusException(400, "Invalid submission: ${e.message}", ctx)
            }

            try {
                runManager.postSubmission(rac, apiSubmission)
            } catch (e: SubmissionRejectedException) {
                throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
            } catch (e: IllegalRunStateException) {
                logger.info("Submission was received while run manager was not accepting submissions.")
                throw ErrorStatusException(
                    400,
                    "Run manager is in wrong state and cannot accept any more submission.",
                    ctx
                )
            } catch (e: IllegalTeamIdException) {
                logger.info("Submission with unknown team id '${rac.teamId}' was received.")
                throw ErrorStatusException(400, "Run manager does not know the given teamId ${rac.teamId}.", ctx)
            }

            DbAuditLogger.submission(apiSubmission, DbAuditLogSource.REST, ctx.sessionToken(), ctx.req().remoteAddr)


            return@transactional SuccessStatus("Submission received")


        }

    }

    private fun transformClientSubmission(
        apiClientSubmission: ApiClientSubmission,
        runManager: RunManager,
        rac: RunActionContext
    ): ApiSubmission {

        if (rac.userId == null || rac.teamId == null) {
            throw Exception("Invalid association between user and evaluation")
        }

        val evaluationRun = runManager.evaluation

        val errorBuffer = StringBuffer()

        val answerSets = apiClientSubmission.answerSets.mapNotNull mapClientAnswerSet@{ clientAnswerSet ->

            val task = if (runManager is InteractiveRunManager) {
                val currentTask = runManager.currentTask(rac) //only accept submissions for current task in interactive runs
                if (currentTask?.isRunning != true) {
                    throw Exception("No active task")
                }
                currentTask
            } else {
                evaluationRun.tasks.find { it.template.name == clientAnswerSet.taskName } //look up task by name
            }

            if (task == null) {
                if (runManager is InteractiveRunManager) {
                    errorBuffer.append("No active task\n")
                } else {
                    errorBuffer.append("task '${clientAnswerSet.taskName}' not found\n")
                }
                return@mapClientAnswerSet null
            }

            val answers = clientAnswerSet.answers.mapNotNull mapClientAnswers@{ clientAnswer ->

                val item = if (clientAnswer.itemName != null) {
                    val collection = if (clientAnswer.itemCollectionName != null) {
                        DbMediaCollection.filter { it.name eq clientAnswer.itemCollectionName }.firstOrNull()
                    } else {
                        task.template.collection
                    }
                    collection?.items?.filter { it.name eq clientAnswer.itemName }?.firstOrNull()
                } else {
                    null
                }?.toApi()

                return@mapClientAnswers when (clientAnswer.type()) {
                    AnswerType.ITEM -> {
                        if (item == null) {
                            errorBuffer.append("item for answer $clientAnswer not found")
                            return@mapClientAnswers null
                        }
                        ApiAnswer(type = ApiAnswerType.ITEM, item = item, start = null, end = null, text = null)
                    }

                    AnswerType.TEMPORAL -> {
                        if (item == null) {
                            errorBuffer.append("item for answer $clientAnswer not found")
                            return@mapClientAnswers null
                        }
                        ApiAnswer(
                            type = ApiAnswerType.TEMPORAL,
                            item = item,
                            start = clientAnswer.start,
                            end = clientAnswer.end,
                            text = null
                        )
                    }

                    AnswerType.TEXT -> ApiAnswer(
                        type = ApiAnswerType.TEXT,
                        text = clientAnswer.text,
                        item = null,
                        start = null,
                        end = null
                    )

                    null -> {
                        errorBuffer.append("could not determine AnswerType of answer $clientAnswer\n")
                        null
                    }
                }

            }

            ApiAnswerSet(
                status = ApiVerdictStatus.INDETERMINATE,
                taskId = task.taskId,
                answers = answers
            )

        }

        if (errorBuffer.isNotBlank()) {
            throw Exception(errorBuffer.toString())
        }

        if (answerSets.isEmpty()) {
            throw Exception("Submission does not contain any AnswerSets")
        }

        val userName = DbUser.filter { it.id eq rac.userId }.first().username
        val teamName = evaluationRun.description.teams.filter { it.id eq rac.teamId }.first().name

        return ApiSubmission(
            teamId = rac.teamId,
            memberId = rac.userId,
            teamName = teamName,
            memberName = userName,
            answers = answerSets,
            evaluationId = evaluationRun.id
        )

    }
}