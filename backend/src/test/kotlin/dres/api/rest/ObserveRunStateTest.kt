package dres.api.rest

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.run.ApiRunProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Ten viewer handlers each spelled out their own version of "a participant may not watch a run that
 * hides its state from participants". They disagreed: every copy tested `isParticipant()`, which excludes
 * judges, so a judge of a closed run was denied nothing, and each had its own message.
 *
 * `AccessManager.canObserveRunState` is now the single statement of the rule, and these tests pin the
 * interpretations that matter. `mediaGrantTest`-style HTTP tests cover the wiring; this covers the rule.
 */
class ObserveRunStateTest {

    private val participantSees = ApiRunProperties(participantCanView = true)
    private val participantHidden = ApiRunProperties(participantCanView = false)

    /** Registers [role] for a fresh session and returns its token. */
    private fun sessionFor(role: ApiRole): String {
        val token = "test-${UUID.randomUUID()}"
        AccessManager.registerUserForSession(
            token,
            ApiUser(id = UUID.randomUUID().toString(), username = "user-$role", role = role)
        )
        return token
    }

    @Test
    fun `a participant sees an open run`() {
        assertTrue(AccessManager.canObserveRunState(sessionFor(ApiRole.PARTICIPANT), participantSees))
    }

    @Test
    fun `a participant cannot see a run that hides itself from participants`() {
        assertFalse(AccessManager.canObserveRunState(sessionFor(ApiRole.PARTICIPANT), participantHidden))
    }

    /**
     * The behaviour the nine hand-rolled copies got wrong: an admin holds PARTICIPANT as well
     * (`registerUserForSession` expands the roles), so a naive `isParticipant()` check denies admins a
     * run they own.
     */
    @Test
    fun `an admin still sees a run that hides itself from participants`() {
        assertTrue(AccessManager.canObserveRunState(sessionFor(ApiRole.ADMIN), participantHidden))
    }

    /** A judge is not a participant, so the participant restriction does not apply. */
    @Test
    fun `a judge sees a run that hides itself from participants`() {
        assertTrue(AccessManager.canObserveRunState(sessionFor(ApiRole.JUDGE), participantHidden))
    }

    @Test
    fun `a viewer sees a run that hides itself from participants`() {
        assertTrue(AccessManager.canObserveRunState(sessionFor(ApiRole.VIEWER), participantHidden))
    }

    /** An unauthenticated session holds no role, so the participant restriction cannot apply. */
    @Test
    fun `an unknown session is not treated as a participant`() {
        assertTrue(AccessManager.canObserveRunState(null as String?, participantHidden))
    }
}
