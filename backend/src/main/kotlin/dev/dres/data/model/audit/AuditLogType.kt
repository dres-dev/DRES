package dev.dres.data.model.audit

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The [AuditLogEntry] types currently supported by DRES.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class AuditLogType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<AuditLogType>() {
        val COMPETITION_START by enumField { description = "COMPETITION_START" }
        val COMPETITION_END by enumField { description = "COMPETITION_END" }
        val TASK_START by enumField { description = "TASK_START" }
        val TASK_MODIFIED by enumField { description = "TASK_MODIFIED" }
        val TASK_END by enumField { description = "TASK_END" }
        val SUBMISSION by enumField { description = "SUBMISSION" }
        val PREPARE_JUDGEMENT by enumField { description = "PREPARE_JUDGEMENT" }
        val JUDGEMENT by enumField { description = "JUDGEMENT" }
        val LOGIN by enumField { description = "LOGIN" }
        val LOGOUT by enumField { description = "LOGOUT" }
        val SUBMISSION_VALIDATION by enumField { description = "SUBMISSION_VALIDATION" }
        val SUBMISSION_STATUS_OVERWRITE by enumField { description = "SUBMISSION_STATUS_OVERWRITE" }
    }

    /** Name / description of the [AuditLogType]. */
    var description by xdRequiredStringProp(unique = true)
        private set
}