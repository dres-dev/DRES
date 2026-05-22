package dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.SubmissionRateFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubmissionRateFilterTest {

    private fun submission(userId: String): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "task1", answers = listOf(ApiClientAnswer(text = "x")))),
            teamId = "team-1",
            userId = userId
        )

    @Test
    fun `first submission from a user is always accepted`() {
        val filter = SubmissionRateFilter(minDelayMs = 500)
        assertTrue(filter.test(submission("user-1")))
    }

    @Test
    fun `second submission before delay elapses is rejected`() {
        val filter = SubmissionRateFilter(minDelayMs = 5000)
        filter.test(submission("user-1")) // first call — accepted, records timestamp
        // immediately call again — not enough time has passed
        assertFalse(filter.test(submission("user-1")))
    }

    @Test
    fun `second submission after delay elapses is accepted`() {
        val filter = SubmissionRateFilter(minDelayMs = 50)
        filter.test(submission("user-1"))
        Thread.sleep(100) // wait longer than the required delay
        assertTrue(filter.test(submission("user-1")))
    }

    @Test
    fun `rate limit is per user — different users do not interfere`() {
        val filter = SubmissionRateFilter(minDelayMs = 5000)
        assertTrue(filter.test(submission("user-1")))
        // user-2 submits immediately after user-1 — should not be affected by user-1's timestamp
        assertTrue(filter.test(submission("user-2")))
    }

    @Test
    fun `third submission immediately after second is also rejected`() {
        val filter = SubmissionRateFilter(minDelayMs = 5000)
        filter.test(submission("user-1"))
        filter.test(submission("user-1")) // rejected, but also resets the timestamp
        assertFalse(filter.test(submission("user-1")))
    }

    @Test
    fun `default delay of 500ms rejects rapid resubmission`() {
        val filter = SubmissionRateFilter() // uses PARAMETER_KEY_DELAY_DEFAULT = 500
        filter.test(submission("user-1"))
        assertFalse(filter.test(submission("user-1")))
    }

    @Test
    fun `default delay of 500ms accepts submission after sufficient wait`() {
        val filter = SubmissionRateFilter()
        filter.test(submission("user-1"))
        Thread.sleep(600)
        assertTrue(filter.test(submission("user-1")))
    }

    @Test
    fun `filter constructed from parameters map uses specified delay`() {
        val filter = SubmissionRateFilter(mapOf(SubmissionRateFilter.PARAMETER_KEY_DELAY to "50"))
        filter.test(submission("user-1"))
        Thread.sleep(100)
        assertTrue(filter.test(submission("user-1")))
    }

    @Test
    fun `filter constructed from parameters map with missing key uses default`() {
        val filter = SubmissionRateFilter(emptyMap())
        filter.test(submission("user-1"))
        // Default is 500ms — immediate resubmission should be rejected
        assertFalse(filter.test(submission("user-1")))
    }
}
