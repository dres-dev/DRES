package dev.dres.run.audit

import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
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

    fun competitionStart(competitionRunUid: UID, api: LogEventSource, session: String?) = log(CompetitionStartAuditLogEntry(competitionRunUid, api, session))

    fun competitionEnd(competitionRunUid: UID, api: LogEventSource, session: String?) = log(CompetitionEndAuditLogEntry(competitionRunUid, api, session))

    fun taskStart(competitionRunUid: UID, taskName: String, api: LogEventSource, session: String?) = log(TaskStartAuditLogEntry(competitionRunUid, taskName, api, session))

    fun taskModified(competitionRunUid: UID, taskName: String, modification: String, api: LogEventSource, session: String?) = log(TaskModifiedAuditLogEntry(competitionRunUid, taskName, modification, api, session))

    fun taskEnd(competitionRunUid: UID, taskName: String, api: LogEventSource, session: String?) = log(TaskEndAuditLogEntry(competitionRunUid, taskName, api, session))

    fun submission(competitionRunUid: UID, taskName: String, submission: Submission, api: LogEventSource, session: String?, address: String) = log(SubmissionAuditLogEntry(competitionRunUid, taskName, submission, api, session, address))

    fun judgement(competitionRunUid: UID, validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, session: String?) = log(JudgementAuditLogEntry(competitionRunUid, validator, token, verdict, api, session))

    fun login(user: String, session: String, api: LogEventSource) = log(LoginAuditLogEntry(user, session, api))

    fun logout(session: String, api: LogEventSource) = log(LogoutAuditLogEntry(session, api))
}