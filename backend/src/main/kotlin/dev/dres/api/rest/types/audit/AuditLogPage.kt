package dev.dres.api.rest.types.audit


/**
 * A collection of [ApiAuditLogEntry]s with pagination information
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class AuditLogPage(
val index: Int = 0,
val entries: List<ApiAuditLogEntry>,
val timestamp: Long = System.currentTimeMillis(),
val oldest: Long
)

