package dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.ValidTextualSubmissionFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidTextualSubmissionFilterTest {

    private lateinit var filter: ValidTextualSubmissionFilter

    private fun submission(vararg answers: ApiClientAnswer): ApiClientSubmission =
        ApiClientSubmission(
            answerSets = listOf(ApiClientAnswerSet(taskId = "task1", answers = answers.toList())),
            teamId = "team-1",
            userId = "user-1"
        )

    @BeforeEach
    fun setup() {
        filter = ValidTextualSubmissionFilter()
    }

    @Test
    fun `text-only answer passes`() {
        val sub = submission(ApiClientAnswer(text = "some answer"))
        assertTrue(filter.test(sub))
    }

    @Test
    fun `answer without text is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1"))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `answer with null text is rejected`() {
        val sub = submission(ApiClientAnswer(text = null))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `temporal answer without text is rejected`() {
        val sub = submission(ApiClientAnswer(mediaItemName = "video1", start = 1000L, end = 5000L))
        assertFalse(filter.test(sub))
    }

    @Test
    fun `multiple text answers all pass`() {
        val sub = submission(
            ApiClientAnswer(text = "answer one"),
            ApiClientAnswer(text = "answer two")
        )
        assertTrue(filter.test(sub))
    }

    @Test
    fun `one answer without text among text answers causes rejection`() {
        val sub = submission(
            ApiClientAnswer(text = "answer one"),
            ApiClientAnswer(mediaItemName = "video1")
        )
        assertFalse(filter.test(sub))
    }

    @Test
    fun `empty string text passes`() {
        val sub = submission(ApiClientAnswer(text = ""))
        assertTrue(filter.test(sub))
    }
}
