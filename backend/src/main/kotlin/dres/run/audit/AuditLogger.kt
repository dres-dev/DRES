package dres.run.audit

import dres.data.dbo.DAO
import dres.data.model.run.Submission
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

    fun taskStart(taskName: String, api: LogEventSource, user: String?) = log(TaskStartAuditLogEntry(competitionRun, taskName, api, user))

    fun taskModified(taskName: String, modification: String, api: LogEventSource, user: String?) = log(TaskModifiedAuditLogEntry(competitionRun, taskName, modification, api, user))

    fun taskEnd(taskName: String, api: LogEventSource, user: String?) = log(TaskEndAuditLogEntry(competitionRun, taskName, api, user))

    fun submission(taskName: String, submission: Submission, api: LogEventSource, user: String?) = log(SubmissionAuditLogEntry(competitionRun, taskName, "[${submission.timestamp}] ${submission.team}.${submission.member}: ${submission.item} (${submission.start} - ${submission.end})", api, user))

    fun judgement(judgementId: Long, api: LogEventSource, user: String?) = log(JudgementAuditLogEntry(competitionRun, judgementId, api, user))
}