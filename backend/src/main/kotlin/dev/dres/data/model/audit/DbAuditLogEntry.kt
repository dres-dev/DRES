package dev.dres.data.model.audit

import dev.dres.api.rest.types.audit.*
import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredDateTimeProp
import kotlinx.dnq.xdStringProp
import org.joda.time.DateTime

/**
 *
 */
class DbAuditLogEntry(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbAuditLogEntry>()

    override fun constructor() {
        super.constructor()
        this.timestamp = DateTime.now()
    }

    /** The type of [DbAuditLogEntry]. */
    var type by xdLink1(DbAuditLogType)

    /** The [DbAuditLogSource] that generated this [DbAuditLogEntry]. */
    var source by xdLink1(DbAuditLogSource)

    /** The timestamp of this [DbAuditLogEntry]. */
    var timestamp by xdRequiredDateTimeProp()

    /** The ID of the evaluation this [DbAuditLogEntry] belongs to. */
    var evaluationId by xdStringProp()

    /** The ID of the task this [DbAuditLogEntry] belongs to. */
    var taskId by xdStringProp()

    /** The ID of the submission this [DbAuditLogEntry] belongs to. Only valid if [type] is equal to [DbAuditLogType.SUBMISSION], [DbAuditLogType.SUBMISSION_VALIDATION] or [DbAuditLogType.SUBMISSION_STATUS_OVERWRITE]. */
    var submissionId by xdStringProp()

    /** The user ID of the user who generated this [DbAuditLogEntry].  Only set if [source] is equal to [DbAuditLogSource.REST]. */
    var userId by xdStringProp()

    /** The session ID of the [DbAuditLogEntry]. Only set if [source] is equal to [DbAuditLogSource.REST]. */
    var session by xdStringProp()

    /** The source address of the [DbAuditLogEntry]. Only set if [source] is equal to [DbAuditLogSource.REST]. */
    var address by xdStringProp()

    /** Descriptive metadata for this [DbAuditLogEntry]. */
    var description by xdStringProp()

    /**
     * Converts this [DbAuditLogEntry] to a RESTful API representation [ApiAuditLogEntry].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiAuditLogEntry]
     */
    fun toApi(): ApiAuditLogEntry = ApiAuditLogEntry(this.id, this.type.toApi(), this.source.toApi(), this.timestamp.millis, this.evaluationId, this.userId, this.submissionId, this.session, this.address, this.description)
}