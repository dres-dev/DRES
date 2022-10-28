package dev.dres.api.rest.types.audit

import dev.dres.data.model.audit.AuditLogSource

/**
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiAuditLogSource(val source: AuditLogSource) {
    REST(AuditLogSource.REST),
    CLI(AuditLogSource.CLI),
    INTERNAL(AuditLogSource.INTERNAL)
}