package dev.dres.data.model.audit

import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredDateTimeProp
import kotlinx.dnq.xdStringProp

class AuditLogEntry(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<AuditLogEntry>()

    /** The type of [AuditLogEntry]. */
    var type by xdLink1(AuditLogType)

    /** The [AuditLogSource] that generated this [AuditLogEntry]. */
    var source by xdLink1(AuditLogSource)

    /** The timestamp of this [AuditLogEntry]. */
    var timestamp by xdRequiredDateTimeProp()

    /** The ID of the competition this [AuditLogEntry] belongs to. */
    var competitionId by xdStringProp()

    /** The ID of the competition this [AuditLogEntry] belongs to. */
    var taskId by xdStringProp()

    /** The ID of the submission this [AuditLogEntry] belongs to. Only valid if [type] is equal to [AuditLogType.SUBMISSION], [AuditLogType.SUBMISSION_VALIDATION] or [AuditLogType.SUBMISSION_STATUS_OVERWRITE]. */
    var submissionId by xdStringProp()

    /** The submission verdicht this captured by this [AuditLogEntry]. Only valid if [type] is equal to [AuditLogType.SUBMISSION_VALIDATION] or [AuditLogType.SUBMISSION_STATUS_OVERWRITE]. */
    var verdict by xdStringProp()

    /** The name of the submission validator.  Only valid if [type] is equal to [AuditLogType.SUBMISSION_VALIDATION]. */
    var validatorName by xdStringProp()

    /** The user ID of the user who generated this [AuditLogEntry].  Only set if [source] is equal to [AuditLogSource.REST]. */
    var userId by xdStringProp()

    /** The session ID of the [AuditLogEntry]. Only set if [source] is equal to [AuditLogSource.REST]. */
    var session by xdStringProp()

    /** The source address of the [AuditLogEntry]. Only set if [source] is equal to [AuditLogSource.REST]. */
    var address by xdStringProp()

    /** Descriptive metadata for this [AuditLogEntry]. */
    var description by xdStringProp()
}