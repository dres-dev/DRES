package dev.dres.run.audit

import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.SubmissionValidator
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object AuditLogger {

    private val logMarker: Marker = MarkerFactory.getMarker("AUDIT")

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private lateinit var dao: DAO<AuditLogEntry>

    fun init(dao: DAO<AuditLogEntry>) {
        this.dao = dao
    }

    private fun log(entry: AuditLogEntry){
        dao.append(entry)
        logger.info(logMarker, "Audit event: $entry")
    }

    fun competitionStart(competitionRunUid: UID, competitionDescription: CompetitionDescription, api: LogEventSource, session: String?) {
        log(CompetitionStartAuditLogEntry(competitionRunUid, api, session))
        EventStreamProcessor.event(
            RunStartEvent(competitionRunUid, competitionDescription)
        )
    }

    fun competitionEnd(competitionRunUid: UID, api: LogEventSource, session: String?) {
        log(CompetitionEndAuditLogEntry(competitionRunUid, api, session))
        EventStreamProcessor.event(
            RunEndEvent(competitionRunUid)
        )
    }

    fun taskStart(competitionRunUid: UID, taskId: TaskId, taskDescription: TaskDescription, api: LogEventSource, session: String?) {
        log(TaskStartAuditLogEntry(competitionRunUid, taskDescription.name, api, session))
        EventStreamProcessor.event(
            TaskStartEvent(
                competitionRunUid,
                taskId,
                taskDescription
            )
        )
    }

    fun taskModified(competitionRunUid: UID, taskName: String, modification: String, api: LogEventSource, session: String?) = log(TaskModifiedAuditLogEntry(competitionRunUid, taskName, modification, api, session))

    fun taskEnd(competitionRunUid: UID, taskId: TaskId, taskDescription: TaskDescription, api: LogEventSource, session: String?) {
        log(TaskEndAuditLogEntry(competitionRunUid, taskDescription.name, api, session))
        EventStreamProcessor.event(
            TaskEndEvent(competitionRunUid, taskId)
        )
    }

    fun submission(competitionRunUid: UID, taskName: String, taskId: TaskId?, submission: Submission, api: LogEventSource, session: String?, address: String) {
        log(SubmissionAuditLogEntry(competitionRunUid, taskName, submission, api, session, address))
        EventStreamProcessor.event(
            SubmissionEvent(session ?: "N/A", competitionRunUid, taskId, submission)
        )
    }

    fun validateSubmission(submission: Submission, validator: SubmissionValidator) = log(SubmissionValidationAuditLogEntry(submission, validator::class.simpleName ?: "unknown validator", submission.status))

    fun prepareJudgement(validator: String, token: String, submission: Submission) = log(PrepareJudgementAuditLogEntry(validator, token, submission))

    fun judgement(competitionRunUid: UID, validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, session: String?) = log(JudgementAuditLogEntry(competitionRunUid, validator, token, verdict, api, session))

    fun login(user: String, session: String, api: LogEventSource) = log(LoginAuditLogEntry(user, session, api))

    fun logout(session: String, api: LogEventSource) = log(LogoutAuditLogEntry(session, api))

    fun overrideSubmission(competitionRunUid: UID, submissionId: UID, newVerdict: SubmissionStatus, api: LogEventSource, session: String?) = log(SubmissionStatusOverwriteAuditLogEntry(competitionRunUid, submissionId, newVerdict, api, session))
}