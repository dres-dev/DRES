package dres.api.rest.types.evaluation

import dev.dres.api.rest.types.evaluation.ApiTaskOverview
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.run.InteractiveRunManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The remaining time used to be re-derived in the frontend, which ignored `COUNTDOWN_DURATION` and drifted
 * from what the viewer showed. It is now computed once, here, so these tests pin the definition.
 */
class ApiTaskOverviewTest {

    private val start = 1_000_000L

    @Test
    fun `a perpetual task reports no deadline`() {
        assertNull(ApiTaskOverview.remainingSeconds(duration = null, started = start, status = ApiTaskStatus.RUNNING, now = start))
    }

    /** A perpetual task run that already ended must not suddenly report a countdown either. */
    @Test
    fun `a perpetual task reports no deadline once it ended`() {
        assertNull(ApiTaskOverview.remainingSeconds(duration = null, started = start, status = ApiTaskStatus.ENDED, now = start))
    }

    @Test
    fun `a running task counts down including the preparation countdown`() {
        val duration = 30L
        val remaining = ApiTaskOverview.remainingSeconds(
            duration = duration,
            started = start,
            status = ApiTaskStatus.RUNNING,
            now = start + 10_000L
        )

        val expected = (duration * 1000L - 10_000L + InteractiveRunManager.COUNTDOWN_DURATION) / 1000L
        assertEquals(expected, remaining)
    }

    @Test
    fun `a task that is not running reports minus one`() {
        for (status in listOf(ApiTaskStatus.CREATED, ApiTaskStatus.PREPARING, ApiTaskStatus.ENDED, ApiTaskStatus.IGNORED)) {
            assertEquals(
                -1L,
                ApiTaskOverview.remainingSeconds(duration = 30L, started = start, status = status, now = start),
                "Status $status must not report a countdown"
            )
        }
    }

    /** Remaining time is clamped at zero, so a task that overran does not report a negative countdown. */
    @Test
    fun `an overrun task reports zero rather than a negative value`() {
        assertEquals(
            0L,
            ApiTaskOverview.remainingSeconds(
                duration = 10L,
                started = start,
                status = ApiTaskStatus.RUNNING,
                now = start + 60_000L
            )
        )
    }

    @Test
    fun `a running task without a start time reports minus one`() {
        assertEquals(-1L, ApiTaskOverview.remainingSeconds(duration = 10L, started = null, status = ApiTaskStatus.RUNNING, now = start))
    }
}
