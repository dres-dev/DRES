package dev.dres.api.rest.types.audit

import dev.dres.api.rest.types.AbstractRestEntity
import dev.dres.api.rest.types.run.SubmissionInfo
import dev.dres.data.model.UID
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.audit.*

sealed class RestAuditLogEntry(val type: AuditLogEntryType, id: UID, val timestamp: Long) : AbstractRestEntity(id.string) {
    companion object {
        fun convert(log: AuditLogEntry): RestAuditLogEntry {
            return when (log) {
                is CompetitionStartAuditLogEntry -> RestCompetitionStartAuditLogEntry(log)
                is CompetitionEndAuditLogEntry -> RestCompetitionEndAuditLogEntry(log)
                is TaskStartAuditLogEntry -> RestTaskStartAuditLogEntry(log)
                is TaskModifiedAuditLogEntry -> RestTaskModifiedAuditLogEntry(log)
                is TaskEndAuditLogEntry -> RestTaskEndAuditLogEntry(log)
                is SubmissionAuditLogEntry -> RestSubmissionAuditLogEntry(log)
                is JudgementAuditLogEntry -> RestJudgementAuditLogEntry(log)
                is LoginAuditLogEntry -> RestLoginAuditLogEntry(log)
                is LogoutAuditLogEntry -> RestLogoutAuditLogEntry(log)
            }
        }
    }
}

class RestCompetitionStartAuditLogEntry(id: UID, timestamp: Long, val competition: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.COMPETITION_START, id, timestamp) {
    constructor(log: CompetitionStartAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.api, log.user)
}

class RestCompetitionEndAuditLogEntry(id: UID, timestamp: Long, val competition: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.COMPETITION_END, id, timestamp) {
    constructor(log: CompetitionEndAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.api, log.user)
}

class RestTaskStartAuditLogEntry(id: UID, timestamp: Long, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_START, id, timestamp) {
    constructor(log: TaskStartAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.taskName, log.api, log.user)
}

class RestTaskModifiedAuditLogEntry(id: UID, timestamp: Long, val competition: String, val taskName: String, val modification: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_MODIFIED, id, timestamp) {
    constructor(log: TaskModifiedAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.taskName, log.modification, log.api, log.user)
}

class RestTaskEndAuditLogEntry(id: UID, timestamp: Long, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_END, id, timestamp) {
    constructor(log: TaskEndAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.taskName, log.api, log.user)
}

class RestSubmissionAuditLogEntry(id: UID, timestamp: Long, val competition: String, val taskName: String, val submission: SubmissionInfo, val api: LogEventSource, val user: String?, val address: String) : RestAuditLogEntry(AuditLogEntryType.SUBMISSION, id, timestamp) {
    constructor(log: SubmissionAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.taskName, SubmissionInfo(log.submission), log.api, log.user, log.address)
}

class RestJudgementAuditLogEntry(id: UID, timestamp: Long, val competition: String, val validator: String, val token: String, val verdict: SubmissionStatus, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.JUDGEMENT, id, timestamp) {
    constructor(log: JudgementAuditLogEntry) : this(log.id, log.timestamp, log.competition.string, log.validator, log.token, log.verdict, log.api, log.user)
}

class RestLoginAuditLogEntry(id: UID, timestamp: Long, val user: String, val session: String, val api: LogEventSource) : RestAuditLogEntry(AuditLogEntryType.LOGIN, id, timestamp) {
    constructor(log: LoginAuditLogEntry) : this(log.id, log.timestamp, log.user, log.session, log.api)
}

class RestLogoutAuditLogEntry(id: UID, timestamp: Long, val session: String, val api: LogEventSource) : RestAuditLogEntry(AuditLogEntryType.LOGOUT, id, timestamp) {
    constructor(log: LogoutAuditLogEntry) : this(log.id, log.timestamp, log.session, log.api)
}