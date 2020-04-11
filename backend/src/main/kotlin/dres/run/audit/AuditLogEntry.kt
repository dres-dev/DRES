package dres.run.audit

import dres.data.model.Entity
import kotlinx.serialization.Serializable

enum class AuditLogEntryType {

    COMPETITION_START,
    COMPETITION_END,
    TASK_START,
    TASK_MODIFIED,
    TASK_END,
    SUBMISSION,
    JUDGEMENT

}

enum class LogEventSource {
    REST,
    CLI,
    GRPC
}

@Serializable
sealed class AuditLogEntry(val type: AuditLogEntryType): Entity{

    var timestamp: Long = System.currentTimeMillis()
        internal set


}

@Serializable
class CompetitionStartAuditLogEntry(override var id: Long, val competition: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_START){
    constructor(competition: String, api: LogEventSource, user: String?): this(-1, competition, api, user)
}

@Serializable
data class CompetitionEndAuditLogEntry(override var id: Long, val competition: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.COMPETITION_END){
    constructor(competition: String, api: LogEventSource, user: String?): this(-1, competition, api, user)
}

@Serializable
data class TaskStartAuditLogEntry(override var id: Long, val competition: String, val taskId: Int, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_START){
    constructor(competition: String, taskId: Int, api: LogEventSource, user: String?): this(-1, competition, taskId, api, user)
}

@Serializable
data class TaskModifiedAuditLogEntry(override var id: Long, val competition: String, val taskId: Int, val modification: String, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_MODIFIED) {
    constructor(competition: String, taskId: Int, modification: String, api: LogEventSource, user: String?): this(-1, competition, taskId, modification, api, user)
}

@Serializable
data class TaskEndAuditLogEntry(override var id: Long, val competition: String, val taskId: Int, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.TASK_END){
    constructor(competition: String, taskId: Int, api: LogEventSource, user: String?): this(-1, competition, taskId, api, user)
}

@Serializable
data class SubmissionAuditLogEntry(override var id: Long, val competition: String, val taskId: Int, val submissionId: Long, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.SUBMISSION){
    constructor(competition: String, taskId: Int, submissionId: Long, api: LogEventSource, user: String?): this(-1, competition, taskId, submissionId, api, user)
}

@Serializable
data class JudgementAuditLogEntry(override var id: Long, val competition: String, val judgementId: Long, val api: LogEventSource, val user: String?) : AuditLogEntry(AuditLogEntryType.JUDGEMENT) {
    constructor(competition: String, judgementId: Long, api: LogEventSource, user: String?): this(-1, competition, judgementId, api, user)
}

