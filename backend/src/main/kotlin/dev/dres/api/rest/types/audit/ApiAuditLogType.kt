package dev.dres.api.rest.types.audit

import dev.dres.data.model.audit.DbAuditLogType

/**
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiAuditLogType {
    COMPETITION_START, COMPETITION_END, TASK_START, TASK_MODIFIED, TASK_END, SUBMISSION,
    PREPARE_JUDGEMENT, JUDGEMENT, LOGIN, LOGOUT, SUBMISSION_VALIDATION, SUBMISSION_STATUS_OVERWRITE;


    /**
     * Converts this [ApiAuditLogType] to a RESTful API representation [DbAuditLogType].
     *
     * @return [DbAuditLogType]
     */
    fun toDb(): DbAuditLogType = when(this) {
        COMPETITION_START -> DbAuditLogType.COMPETITION_START
        COMPETITION_END -> DbAuditLogType.COMPETITION_END
        TASK_START -> DbAuditLogType.TASK_START
        TASK_MODIFIED -> DbAuditLogType.TASK_MODIFIED
        TASK_END -> DbAuditLogType.TASK_END
        SUBMISSION -> DbAuditLogType.SUBMISSION
        PREPARE_JUDGEMENT -> DbAuditLogType.PREPARE_JUDGEMENT
        JUDGEMENT -> DbAuditLogType.JUDGEMENT
        LOGIN -> DbAuditLogType.LOGIN
        LOGOUT -> DbAuditLogType.LOGOUT
        SUBMISSION_VALIDATION -> DbAuditLogType.SUBMISSION_VALIDATION
        SUBMISSION_STATUS_OVERWRITE -> DbAuditLogType.SUBMISSION_STATUS_OVERWRITE
    }
}