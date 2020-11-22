package dev.dres.api.rest.types.audit

import dev.dres.api.rest.types.AbstractRestEntity
import dev.dres.api.rest.types.run.SubmissionInfo
import dev.dres.data.model.UID
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.audit.*

sealed class RestAuditLogEntry(val type: AuditLogEntryType, id: UID) : AbstractRestEntity(id.string) {
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

class RestCompetitionStartAuditLogEntry(id: UID, val competition: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.COMPETITION_START, id) {
    constructor(competition: UID, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, api, user)
    constructor(log: CompetitionStartAuditLogEntry) : this(log.id, log.competition.string, log.api, log.user)
}

class RestCompetitionEndAuditLogEntry(id: UID, val competition: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.COMPETITION_END, id) {
    constructor(competition: UID, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, api, user)
    constructor(log: CompetitionEndAuditLogEntry) : this(log.id, log.competition.string, log.api, log.user)
}

class RestTaskStartAuditLogEntry(id: UID, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_START, id) {
    constructor(competition: UID, taskName: String, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, taskName, api, user)
    constructor(log: TaskStartAuditLogEntry) : this(log.id, log.competition.string, log.taskName, log.api, log.user)
}

class RestTaskModifiedAuditLogEntry(id: UID, val competition: String, val taskName: String, val modification: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_MODIFIED, id) {
    constructor(competition: UID, taskName: String, modification: String, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, taskName, modification, api, user)
    constructor(log: TaskModifiedAuditLogEntry) : this(log.id, log.competition.string, log.taskName, log.modification, log.api, log.user)
}

class RestTaskEndAuditLogEntry(id: UID, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.TASK_END, id) {
    constructor(competition: UID, taskName: String, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, taskName, api, user)
    constructor(log: TaskEndAuditLogEntry) : this(log.id, log.competition.string, log.taskName, log.api, log.user)
}

class RestSubmissionAuditLogEntry(id: UID, val competition: String, val taskName: String, val submission: SubmissionInfo, val api: LogEventSource, val user: String?, val address: String) : RestAuditLogEntry(AuditLogEntryType.SUBMISSION, id) {
    constructor(competition: UID, taskName: String, submission: Submission, api: LogEventSource, user: String?, address: String) : this(UID.EMPTY, competition.string, taskName, SubmissionInfo(submission), api, user, address)
    constructor(log: SubmissionAuditLogEntry) : this(log.id, log.competition.string, log.taskName, SubmissionInfo(log.submission), log.api, log.user, log.address)
}

class RestJudgementAuditLogEntry(id: UID, val competition: String, val validator: String, val token: String, val verdict: SubmissionStatus, val api: LogEventSource, val user: String?) : RestAuditLogEntry(AuditLogEntryType.JUDGEMENT, id) {
    constructor(competition: UID, validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, user: String?) : this(UID.EMPTY, competition.string, validator, token, verdict, api, user)
    constructor(log: JudgementAuditLogEntry) : this(log.id, log.competition.string, log.validator, log.token, log.verdict, log.api, log.user)
}

class RestLoginAuditLogEntry(id: UID, val user: String, val session: String, val api: LogEventSource) : RestAuditLogEntry(AuditLogEntryType.LOGIN, id) {
    constructor(user: String, session: String, api: LogEventSource) : this(UID.EMPTY, user, session, api)
    constructor(log: LoginAuditLogEntry) : this(log.id, log.user, log.session, log.api)
}

class RestLogoutAuditLogEntry(id: UID, val session: String, val api: LogEventSource) : RestAuditLogEntry(AuditLogEntryType.LOGOUT, id) {
    constructor(session: String, api: LogEventSource) : this(UID.EMPTY, session, api)
    constructor(log: LogoutAuditLogEntry) : this(log.id, log.session, log.api)
}