package dev.dres.run.audit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.DRES
import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.TemplateId

import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object AuditLogger {

    private const val FLUSH_INTERVAL = 30_000 //flush every 30 seconds

    private val logMarker: Marker = MarkerFactory.getMarker("AUDIT")

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val queue = LinkedBlockingQueue<AuditLogEntry>()

    private var active = true


    init {
        thread(
            name = "AuditLogHelperThread",
            isDaemon = true,
            start = true
        ) {
            val mapper = jacksonObjectMapper()
            Files.createDirectories(DRES.AUDIT_LOG_ROOT)
            val auditLogFile = DRES.AUDIT_LOG_ROOT.resolve("audit.jsonl").toFile()
            val writer = PrintWriter(FileWriter(auditLogFile, Charsets.UTF_8, true))
            var lastFlush = 0L
            while (active || queue.isNotEmpty()) {

                try {

                    val logEntry = queue.poll(1, TimeUnit.SECONDS) ?: continue
                    writer.println(
                        mapper.writeValueAsString(logEntry)
                    )

                    val now = System.currentTimeMillis()

                    if (now - lastFlush > FLUSH_INTERVAL) {
                        writer.flush()
                        lastFlush = now
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            writer.flush()
            writer.close()

        }
    }

    fun stop() {
        log(ShutdownAuditLogEntry())
        active = false
    }

    private fun log(entry: AuditLogEntry) {
        queue.add(entry)
        logger.info(logMarker, "Audit event: $entry")
    }

    /**
     * Logs the start of a DRES competition.
     *
     * @param description The [ApiEvaluationTemplate].
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun evaluationStart(
        evaluationId: EvaluationId,
        description: ApiEvaluationTemplate,
        api: AuditLogSource,
        userId: UserId?,
        session: SessionToken?
    ) {
        log(EvaluationStartAuditLogEntry(evaluationId, description, api, userId, session))
        EventStreamProcessor.event(RunStartEvent(evaluationId, description))
    }

    /**
     * Logs the end of a DRES competition.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun evaluationEnd(evaluationId: EvaluationId, api: AuditLogSource, userId: UserId?, session: SessionToken?) {
        log(EvaluationEndAuditLogEntry(evaluationId, api, userId, session))
        EventStreamProcessor.event(RunEndEvent(evaluationId))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param description The [ApiTaskTemplate].
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskStart(
        evaluationId: EvaluationId,
        taskId: EvaluationId,
        description: ApiTaskTemplate,
        api: AuditLogSource,
        session: SessionToken?
    ) {
        log(TaskStartAuditLogEntry(evaluationId, taskId, description, api, session))
        EventStreamProcessor.event(TaskStartEvent(evaluationId, taskId, description))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param modification Description of the modification.
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskModified(
        evaluationId: EvaluationId,
        taskId: EvaluationId,
        modification: String,
        api: AuditLogSource,
        session: String?
    ) {
        log(TaskModifiedAuditLogEntry(evaluationId, taskId, modification, api, session))
    }

    /**
     * Logs the end of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskEnd(evaluationId: EvaluationId, taskId: TemplateId, api: AuditLogSource, session: SessionToken?) {
        log(TaskEndAuditLogEntry(evaluationId, taskId, api, session))
        EventStreamProcessor.event(TaskEndEvent(evaluationId, taskId))
    }

    /**
     * Logs an incoming submission to DRES.
     *
     * @param submission The [ApiClientSubmission] that was registered.
     * @param api The [AuditLogSource]
     * @param sessionToken The identifier of the user session.
     * @param address The IP address of the submitter.
     */
    fun submission(
        submission: ApiClientSubmission,
        evaluationId: EvaluationId,
        api: AuditLogSource,
        sessionToken: SessionToken?,
        address: String
    ) {
        log(SubmissionAuditLogEntry(submission, evaluationId, api, sessionToken, address))
        EventStreamProcessor.event(SubmissionEvent(sessionToken ?: "na", evaluationId, submission))
    }

    /**
     * Logs a AnswerSet VerdictStatus override to DRES.
     */
    fun overrideVerdict(
        answerSet: ApiAnswerSet,
        verdict: ApiVerdictStatus,
        api: AuditLogSource,
        sessionToken: SessionToken?
    ) {
        log(OverrideVerdictAuditLogEntry(answerSet, verdict, api, sessionToken))
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param answerSet The [ApiAnswerSet] that was overwritten (new snapshot).
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     */
    fun prepareJudgement(answerSet: ApiAnswerSet, validator: JudgementValidator, token: String) {
        log(PrepareJudgementAuditLogEntry(answerSet, validator, token))
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     * @param verdict The [ApiVerdictStatus] submitted by the judge.
     * @param api The [AuditLogSource]
     * @param sessionToken The identifier of the user session.
     */
    fun judgement(
        evaluationId: EvaluationId,
        validator: JudgementValidator,
        token: String,
        verdict: ApiVerdictStatus,
        api: AuditLogSource,
        sessionToken: SessionToken?
    ) {
        log(JudgementAuditLogEntry(evaluationId, validator, token, verdict, api, sessionToken))
    }

    /**
     * Logs a user user login event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [AuditLogSource]
     * @param sessionToken The [SessionToken]
     */
    fun login(userId: UserId, api: AuditLogSource, sessionToken: SessionToken) {
        log(LoginAuditLogEntry(userId, api, sessionToken))
    }

    /**
     * Logs a user logout event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [AuditLogSource]
     * @param sessionToken The [SessionToken]
     */
    fun logout(userId: UserId, api: AuditLogSource, sessionToken: SessionToken) {
        log(LogoutAuditLogEntry(userId, api, sessionToken))
    }

    fun startup() {
        log(StartupAuditLogEntry())
    }
}