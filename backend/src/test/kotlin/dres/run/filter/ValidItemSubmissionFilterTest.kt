package dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.ValidItemSubmissionFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidItemSubmissionFilterTest {

    private lateinit var filter: ValidItemSubmissionFilter

    private fun submission(vararg answers: ApiClientAnswer): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "task1", answers = answers.toList())),
            teamId = "team-1",
            userId = "user-1"
        )

    @BeforeEach
    fun setup() {
        filter = ValidItemSubmissionFilter()
    }

    @Test
    fun `item-only answer passes`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1"))
        assertTrue(filter.test(sub))
    }

    @Test
    fun `answer with start timestamp is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer with end timestamp is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", end = 5000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer with both timestamps is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L, end = 5000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer with text is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", text = "some text"))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer without media item name is rejected`() {
        val sub = submission(ApiClientAnswer())
        assertFalse(filter.test(sub))
    }

    @Test
    fun `multiple item-only answers all pass`() {
        val sub = submission(
            ApiClientAnswer(mediaItemName = "video1"),
            ApiClientAnswer(mediaItemName = "video2")
        )
        assertTrue(filter.test(sub))
    }

    @Test
    fun `mixed answers where one has a timestamp causes rejection`() {
        val sub = submission(
            ApiClientAnswer(mediaItemName = "video1"),
            ApiClientAnswer(mediaItemName = "video2", start = 1000L, end = 5000L)
        )
        assertFalse(filter.test(sub))
    }

    @Test
    fun `empty answer set passes`() {
        val sub = ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "task1", answers = emptyList())),
            teamId = "team-1",
            userId = "user-1"
        )
        assertTrue(filter.test(sub))
    }
}
