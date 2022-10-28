package dev.dres.data.model.audit

import dev.dres.api.rest.types.audit.*
import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredDateTimeProp
import kotlinx.dnq.xdStringProp

/**
 *
 */
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

    /** The user ID of the user who generated this [AuditLogEntry].  Only set if [source] is equal to [AuditLogSource.REST]. */
    var userId by xdStringProp()

    /** The session ID of the [AuditLogEntry]. Only set if [source] is equal to [AuditLogSource.REST]. */
    var session by xdStringProp()

    /** The source address of the [AuditLogEntry]. Only set if [source] is equal to [AuditLogSource.REST]. */
    var address by xdStringProp()

    /** Descriptive metadata for this [AuditLogEntry]. */
    var description by xdStringProp()

    /**
     * Converts this [AuditLogEntry] to a RESTful API representation [ApiAuditLogEntry].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiAuditLogEntry]
     */
    fun toApi(): ApiAuditLogEntry = ApiAuditLogEntry(this.id, this.type.toApi(), this.source.toApi(), this.timestamp.millis, this.competitionId, this.userId, this.submissionId, this.session, this.address, this.description)
}