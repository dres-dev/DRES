package dres.integration

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccessManagerTest : AbstractDresIntegrationTest() {
    @Test
    fun `logging into another account replaces roles for existing session`() {
        val sessionId = "session-${java.util.UUID.randomUUID()}"
        val admin = ApiUser(id = "admin-id", username = "admin", role = ApiRole.ADMIN)
        val participant = ApiUser(id = "participant-id", username = "participant", role = ApiRole.PARTICIPANT)

        try {
            AccessManager.registerUserForSession(sessionId, admin)
            assertEquals(setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN), AccessManager.rolesOfSession(sessionId))

            AccessManager.registerUserForSession(sessionId, participant)

            assertEquals(participant.id, AccessManager.userIdForSession(sessionId))
            assertEquals(setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT), AccessManager.rolesOfSession(sessionId))
        } finally {
            AccessManager.deregisterUserSession(sessionId)
        }
    }

}
