package dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.ValidTemporalSubmissionFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidTemporalSubmissionFilterTest {

    private lateinit var filter: ValidTemporalSubmissionFilter

    private fun submission(vararg answers: ApiClientAnswer): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "task1", answers = answers.toList())),
            teamId = "team-1",
            userId = "user-1"
        )

    @BeforeEach
    fun setup() {
        filter = ValidTemporalSubmissionFilter()
    }

    @Test
    fun `temporal answer with media item, start and end passes`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L, end = 5000L))
        assertTrue(filter.test(sub))
    }

    @Test
    fun `answer missing media item is rejected`() {
        val sub = submission(ApiClientAnswer(start = 1000L, end = 5000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer missing start is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", end = 5000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer missing end is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer where start equals end passes`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L, end = 1000L))
        assertTrue(filter.test(sub))
    }

    @Test
    fun `answer where start is after end is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 5000L, end = 1000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `item-only answer without timestamps is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1"))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `text-only answer is rejected`() {
        val sub = submission(ApiClientAnswer(text = "some text"))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `multiple valid temporal answers pass`() {
        val sub = submission(
            ApiClientAnswer(mediaItemName = "video1", start = 0L, end = 1000L),
            ApiClientAnswer(mediaItemName = "video2", start = 2000L, end = 3000L)
        )
        assertTrue(filter.test(sub))
    }

    @Test
    fun `one invalid answer among valid ones causes rejection`() {
        val sub = submission(
            ApiClientAnswer(mediaItemName = "video1", start = 0L, end = 1000L),
            ApiClientAnswer(mediaItemName = "video2")  // missing timestamps
        )
        assertFalse(filter.test(sub))
    }

    @Test
    fun `start of zero is valid`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 0L, end = 1000L))
        assertTrue(filter.test(sub))
    }
}
