package dev.dres.api.rest.types.audit

data class AuditLogInfo(
        val timestamp: Long = System.currentTimeMillis(),
        val size: Int,
        val latest: Long? = null
)