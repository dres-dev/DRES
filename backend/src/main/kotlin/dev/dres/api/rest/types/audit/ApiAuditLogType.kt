package dev.dres.api.rest.types.audit

import dev.dres.data.model.audit.AuditLogType

/**
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiAuditLogType(val type: AuditLogType) {
    COMPETITION_START(AuditLogType.COMPETITION_START),
    COMPETITION_END(AuditLogType.COMPETITION_END),
    TASK_START(AuditLogType.TASK_START),
    TASK_MODIFIED(AuditLogType.TASK_MODIFIED),
    TASK_END(AuditLogType.TASK_END),
    SUBMISSION(AuditLogType.SUBMISSION),
    PREPARE_JUDGEMENT(AuditLogType.PREPARE_JUDGEMENT),
    JUDGEMENT(AuditLogType.JUDGEMENT),
    LOGIN(AuditLogType.LOGIN),
    LOGOUT(AuditLogType.LOGOUT),
    SUBMISSION_VALIDATION(AuditLogType.SUBMISSION_VALIDATION),
    SUBMISSION_STATUS_OVERWRITE(AuditLogType.SUBMISSION_STATUS_OVERWRITE)
}