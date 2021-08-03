package dev.dres.run.audit

import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus

enum class AuditLogEntryType {

    COMPETITION_START,
    COMPETITION_END,
    TASK_START,
    TASK_MODIFIED,
    TASK_END,
    SUBMISSION,
    PREPARE_JUDGEMENT,
    JUDGEMENT,
    LOGIN,
    LOGOUT,
    SUBMISSION_VALIDATION

}

enum class LogEventSource {
    REST,
    CLI,
    INTERNAL
}

sealed class AuditLogEntry(val type: AuditLogEntryType): Entity{

    var timestamp: Long = System.currentTimeMillis()
        internal set


}

data class CompetitionStartAuditLogEntry(override var id: UID, val competition: UID, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_START){
    constructor(competition: UID, api: LogEventSource, user: String?): this(UID.EMPTY, competition, api, user)
}

data class CompetitionEndAuditLogEntry(override var id: UID, val competition: UID, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_END){
    constructor(competition: UID, api: LogEventSource, user: String?): this(UID.EMPTY, competition, api, user)
}

data class TaskStartAuditLogEntry(override var id: UID, val competition: UID, val taskName: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_START){
    constructor(competition: UID, taskName: String, api: LogEventSource, user: String?): this(UID.EMPTY, competition, taskName, api, user)
}

data class TaskModifiedAuditLogEntry(override var id: UID, val competition: UID, val taskName: String, val modification: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_MODIFIED) {
    constructor(competition: UID, taskName: String, modification: String, api: LogEventSource, user: String?): this(UID.EMPTY, competition, taskName, modification, api, user)
}

data class TaskEndAuditLogEntry(override var id: UID, val competition: UID, val taskName: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_END){
    constructor(competition: UID, taskName: String, api: LogEventSource, user: String?): this(UID.EMPTY, competition, taskName, api, user)
}

data class SubmissionAuditLogEntry(override var id: UID, val competition: UID, val taskName: String, val submission: Submission, val api: LogEventSource, val user: String?, val address: String) : AuditLogEntry(AuditLogEntryType.SUBMISSION){
    constructor(competition: UID, taskName: String, submission: Submission, api: LogEventSource, user: String?, address: String): this(UID.EMPTY, competition, taskName, submission, api, user, address)
}

data class PrepareJudgementAuditLogEntry(override var id: UID, val validator: String, val token: String, val submission: Submission) : AuditLogEntry(AuditLogEntryType.PREPARE_JUDGEMENT){
    constructor(validator: String, token: String, submission: Submission): this(UID.EMPTY, validator, token, submission)
}

data class JudgementAuditLogEntry(override var id: UID, val competition: UID, val validator: String, val token: String, val verdict: SubmissionStatus, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.JUDGEMENT) {
    constructor(competition: UID, validator: String, token: String, verdict: SubmissionStatus, api: LogEventSource, user: String?): this(UID.EMPTY, competition, validator, token, verdict, api, user)
}

data class LoginAuditLogEntry(override var id: UID, val user: String, val session: String, val api: LogEventSource) : AuditLogEntry(AuditLogEntryType.LOGIN) {
    constructor(user: String, session: String, api: LogEventSource): this(UID.EMPTY, user, session, api)
}

data class LogoutAuditLogEntry(override var id: UID, val session: String, val api: LogEventSource) : AuditLogEntry(AuditLogEntryType.LOGOUT) {
    constructor(session: String, api: LogEventSource): this(UID.EMPTY, session, api)
}

data class SubmissionValidationAuditLogEntry(override var id: UID, val submission: Submission, val validatorName: String, val status: SubmissionStatus) : AuditLogEntry(AuditLogEntryType.SUBMISSION_VALIDATION) {
    constructor(submission: Submission, validatorName: String, status: SubmissionStatus) : this(UID.EMPTY, submission, validatorName, status)
}