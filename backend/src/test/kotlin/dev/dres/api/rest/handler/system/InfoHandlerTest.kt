package dev.dres.api.rest.handler.system

import dev.dres.api.rest.types.system.SystemStatus
import dev.dres.api.rest.types.system.SystemStatusLevel
import dev.dres.api.rest.types.users.ApiRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InfoHandlerTest {
    @Test
    fun `includes system status for administrators only`() {
        val status = SystemStatus("TEST_WARNING", SystemStatusLevel.WARNING, "Test warning")
        val handler = InfoHandler { listOf(status) }

        assertEquals(listOf(status), handler.infoForRoles(setOf(ApiRole.ADMIN)).systemStatuses)
        assertEquals(emptyList<SystemStatus>(), handler.infoForRoles(setOf(ApiRole.JUDGE)).systemStatuses)
    }
}
