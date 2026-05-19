package dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.run.filter.ValidItemSubmissionFilter
import dev.dres.run.filter.ValidTemporalSubmissionFilter
import dev.dres.run.filter.ValidTextualSubmissionFilter
import dev.dres.run.filter.basics.AcceptAllSubmissionFilter
import dev.dres.run.filter.basics.AcceptNoneSubmissionFilter
import dev.dres.run.filter.basics.CombiningSubmissionFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CombiningSubmissionFilterTest {

    private fun itemSubmission(): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "t1", answers = listOf(ApiClientAnswer(mediaItemName = "img1")))),
            teamId = "team-1",
            userId = "user-1"
        )

    private fun temporalSubmission(): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "t1", answers = listOf(ApiClientAnswer(mediaItemName = "vid1", start = 0L, end = 5000L)))),
            teamId = "team-1",
            userId = "user-1"
        )

    private fun textSubmission(): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "t1", answers = listOf(ApiClientAnswer(text = "answer")))),
            teamId = "team-1",
            userId = "user-1"
        )

    @Test
    fun `empty filter list accepts everything`() {
        val filter = CombiningSubmissionFilter(emptyList())
        assertTrue(filter.test(itemSubmission()))
        assertTrue(filter.test(temporalSubmission()))
        assertTrue(filter.test(textSubmission()))
    }

    @Test
    fun `single AcceptAll filter accepts any submission`() {
        val filter = CombiningSubmissionFilter(listOf(AcceptAllSubmissionFilter))
        assertTrue(filter.test(itemSubmission()))
    }

    @Test
    fun `single AcceptNone filter rejects any submission`() {
        val filter = CombiningSubmissionFilter(listOf(AcceptNoneSubmissionFilter))
        assertFalse(filter.test(itemSubmission()))
    }

    @Test
    fun `AcceptAll combined with AcceptNone rejects — all must pass`() {
        val filter = CombiningSubmissionFilter(listOf(AcceptAllSubmissionFilter, AcceptNoneSubmissionFilter))
        assertFalse(filter.test(itemSubmission()))
    }

    @Test
    fun `ValidItem combined with AcceptAll accepts item submission`() {
        val filter = CombiningSubmissionFilter(listOf(ValidItemSubmissionFilter(), AcceptAllSubmissionFilter))
        assertTrue(filter.test(itemSubmission()))
    }

    @Test
    fun `ValidItem combined with AcceptAll rejects temporal submission`() {
        val filter = CombiningSubmissionFilter(listOf(ValidItemSubmissionFilter(), AcceptAllSubmissionFilter))
        assertFalse(filter.test(temporalSubmission()))
    }

    @Test
    fun `ValidItem and ValidTemporal together reject all — mutually exclusive`() {
        val filter = CombiningSubmissionFilter(listOf(ValidItemSubmissionFilter(), ValidTemporalSubmissionFilter()))
        assertFalse(filter.test(itemSubmission()))
        assertFalse(filter.test(temporalSubmission()))
    }

    @Test
    fun `acceptOrThrow throws SubmissionRejectedException when filter rejects`() {
        val filter = CombiningSubmissionFilter(listOf(AcceptNoneSubmissionFilter))
        assertThrows(SubmissionRejectedException::class.java) {
            filter.acceptOrThrow(itemSubmission())
        }
    }

    @Test
    fun `acceptOrThrow does not throw when all filters accept`() {
        val filter = CombiningSubmissionFilter(listOf(AcceptAllSubmissionFilter))
        assertDoesNotThrow {
            filter.acceptOrThrow(itemSubmission())
        }
    }

    @Test
    fun `ValidTextual combined with AcceptAll accepts text submission`() {
        val filter = CombiningSubmissionFilter(listOf(ValidTextualSubmissionFilter(), AcceptAllSubmissionFilter))
        assertTrue(filter.test(textSubmission()))
    }

    @Test
    fun `ValidTextual combined with AcceptAll rejects item submission`() {
        val filter = CombiningSubmissionFilter(listOf(ValidTextualSubmissionFilter(), AcceptAllSubmissionFilter))
        assertFalse(filter.test(itemSubmission()))
    }
}
