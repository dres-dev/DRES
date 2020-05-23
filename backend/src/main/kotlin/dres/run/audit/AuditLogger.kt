package dres.run.audit

import dres.data.dbo.DAO
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

class AuditLogger internal constructor(private val competitionRun: String, private val dao: DAO<AuditLogEntry>) {

    companion object{
        val logMarker: Marker = MarkerFactory.getMarker("AUDIT")
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private fun log(entry: AuditLogEntry){
        dao.append(entry)
        logger.info(logMarker, "Audit event in $competitionRun: $entry")
    }

    fun competitionStart(api: LogEventSource, session: String?) = log(CompetitionStartAuditLogEntry(competitionRun, api, session))

    fun competitionEnd(api: LogEventSource, session: String?) = log(CompetitionEndAuditLogEntry(competitionRun, api, session))

    fun taskStart(taskName: String, api: LogEventSource, session: String?) = log(TaskStartAuditLogEntry(competitionRun, taskName, api, session))

    fun taskModified(taskName: String, modification: String, api: LogEventSource, session: String?) = log(TaskModifiedAuditLogEntry(competitionRun, taskName, modification, api, session))

    fun taskEnd(taskName: String, api: LogEventSource, session: String?) = log(TaskEndAuditLogEntry(competitionRun, taskName, api, session))

    fun submission(taskName: String, submission: Submission, api: LogEventSource, session: String?) = log(SubmissionAuditLogEntry(competitionRun, taskName, "[${submission.timestamp}] ${submission.team}.${submission.member}: ${submission.item} (${submission.start} - ${submission.end})", api, session))

    fun judgement(validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, session: String?) = log(JudgementAuditLogEntry(competitionRun, validator, token, verdict, api, session))

    fun login(user: String, session: String, api: LogEventSource) = log(LoginAuditLogEntry(user, session, api))

    fun logout(session: String, api: LogEventSource) = log(LogoutAuditLogEntry(session, api))
}