package dev.dres.api.rest.types.audit

import dev.dres.data.model.audit.AuditLogEntry

/**
 * A RESTful API representation of a [AuditLogEntry]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class ApiAuditLogEntry(
    val id: String,
    val type: ApiAuditLogType,
    val source: ApiAuditLogSource,
    val timestamp: Long,
    val competitionId: String? = null,
    val userId: String? = null,
    val submissionId: String? = null,
    val session: String? = null,
    val address: String? = null,
    val description: String? = null
)