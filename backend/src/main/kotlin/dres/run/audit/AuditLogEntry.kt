package dres.run.audit

import dres.data.model.Entity
import dres.data.model.run.SubmissionStatus

enum class AuditLogEntryType {

    COMPETITION_START,
    COMPETITION_END,
    TASK_START,
    TASK_MODIFIED,
    TASK_END,
    SUBMISSION,
    JUDGEMENT,
    LOGIN,
    LOGOUT

}

enum class LogEventSource {
    REST,
    CLI,
    GRPC
}

sealed class AuditLogEntry(val type: AuditLogEntryType): Entity{

    var timestamp: Long = System.currentTimeMillis()
        internal set


}

data class CompetitionStartAuditLogEntry(override var id: Long, val competition: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_START){
    constructor(competition: String, api: LogEventSource, user: String?): this(-1, competition, api, user)
}

data class CompetitionEndAuditLogEntry(override var id: Long, val competition: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_END){
    constructor(competition: String, api: LogEventSource, user: String?): this(-1, competition, api, user)
}

data class TaskStartAuditLogEntry(override var id: Long, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_START){
    constructor(competition: String, taskName: String, api: LogEventSource, user: String?): this(-1, competition, taskName, api, user)
}

data class TaskModifiedAuditLogEntry(override var id: Long, val competition: String, val taskName: String, val modification: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_MODIFIED) {
    constructor(competition: String, taskName: String, modification: String, api: LogEventSource, user: String?): this(-1, competition, taskName, modification, api, user)
}

data class TaskEndAuditLogEntry(override var id: Long, val competition: String, val taskName: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_END){
    constructor(competition: String, taskName: String, api: LogEventSource, user: String?): this(-1, competition, taskName, api, user)
}

data class SubmissionAuditLogEntry(override var id: Long, val competition: String, val taskName: String, val submissionSummary: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.SUBMISSION){
    constructor(competition: String, taskName: String, submissionSummary: String, api: LogEventSource, user: String?): this(-1, competition, taskName, submissionSummary, api, user)
}

data class JudgementAuditLogEntry(override var id: Long, val competition: String, val validator: String, val token: String, val verdict: SubmissionStatus, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.JUDGEMENT) {
    constructor(competition: String, validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, user: String?): this(-1, competition, validator, token, verdict, api, user)
}

data class LoginAuditLogEntry(override var id: Long, val user: String, val session: String, val api: LogEventSource) : AuditLogEntry(AuditLogEntryType.LOGIN) {
    constructor(user: String, session: String, api: LogEventSource): this(-1, user, session, api)
}

data class LogoutAuditLogEntry(override var id: Long, val session: String, val api: LogEventSource) : AuditLogEntry(AuditLogEntryType.LOGOUT) {
    constructor(session: String, api: LogEventSource): this(-1, session, api)
}