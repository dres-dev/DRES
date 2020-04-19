package dres.run.audit

import dres.data.dbo.DAO
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

    fun competitionStart(api: LogEventSource, user: String?) = log(CompetitionStartAuditLogEntry(competitionRun, api, user))

    fun competitionEnd(api: LogEventSource, user: String?) = log(CompetitionEndAuditLogEntry(competitionRun, api, user))

    fun taskStart(taskId: Int, api: LogEventSource, user: String?) = log(TaskStartAuditLogEntry(competitionRun, taskId, api, user))

    fun taskModified(taskId: Int, modification: String, api: LogEventSource, user: String?) = log(TaskModifiedAuditLogEntry(competitionRun, taskId, modification, api, user))

    fun taskEnd(taskId: Int, api: LogEventSource, user: String?) = log(TaskEndAuditLogEntry(competitionRun, taskId, api, user))

    fun submission(taskId: Int, submissionId: Long, api: LogEventSource, user: String?) = log(SubmissionAuditLogEntry(competitionRun, taskId, submissionId, api, user))

    fun judgement(judgementId: Long, api: LogEventSource, user: String?) = log(JudgementAuditLogEntry(competitionRun, judgementId, api, user))
}