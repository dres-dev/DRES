package dres.run.audit

import dres.data.dbo.DAO

class AuditLogger(private val competition: String, private val dao: DAO<AuditLogEntry>) {

    private fun log(entry: AuditLogEntry){
        dao.append(entry)
    }

    fun competitionStart(api: LogEventSource, user: String?) = log(CompetitionStartAuditLogEntry(competition, api, user))

    fun competitionEnd(api: LogEventSource, user: String?) = log(CompetitionEndAuditLogEntry(competition, api, user))

    fun taskStart(taskId: Int, api: LogEventSource, user: String?) = log(TaskStartAuditLogEntry(competition, taskId, api, user))

    fun taskModified(taskId: Int, modification: String, api: LogEventSource, user: String?) = log(TaskModifiedAuditLogEntry(competition, taskId, modification, api, user))

    fun taskEnd(taskId: Int, api: LogEventSource, user: String?) = log(TaskEndAuditLogEntry(competition, taskId, api, user))

    fun submission(taskId: Int, submissionId: Long, api: LogEventSource, user: String?) = log(SubmissionAuditLogEntry(competition, taskId, submissionId, api, user))

    fun judgement(judgementId: Long, api: LogEventSource, user: String?) = log(JudgementAuditLogEntry(competition, judgementId, api, user))
}