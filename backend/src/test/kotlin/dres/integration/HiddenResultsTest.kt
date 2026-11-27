package dres.integration

import dev.dres.api.rest.handler.evaluation.HiddenResultsVisibility
import dev.dres.api.rest.types.evaluation.submission.ApiAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerType
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HiddenResultsTest {
    @Test
    fun `hidden answers are visible only to the participant's own team`() {
        val submission = ApiSubmission(
            submissionId = "submission-1",
            teamId = "team-own",
            memberId = "participant-1",
            teamName = "Own team",
            memberName = "Participant",
            timestamp = 1L,
            answers = listOf(
                ApiAnswerSet(
                    id = "answer-set-1",
                    status = ApiVerdictStatus.CORRECT,
                    taskId = "task-1",
                    answers = listOf(
                        ApiAnswer(
                            type = ApiAnswerType.TEXT,
                            item = null,
                            text = "secret answer",
                            start = 12L,
                            end = 34L
                        )
                    )
                )
            )
        )

        val ownTeamResult = HiddenResultsVisibility.redactIfRequired(
            hidden = true,
            participant = true,
            submission = submission,
            ownTeamIds = setOf("team-own")
        )
        assertEquals("secret answer", ownTeamResult.answers.single().answers.single().text)

        val otherTeamResult = HiddenResultsVisibility.redactIfRequired(
            hidden = true,
            participant = true,
            submission = submission.copy(teamId = "team-other"),
            ownTeamIds = setOf("team-own")
        )
        assertHidden(otherTeamResult)

        val adminResult = HiddenResultsVisibility.redactIfRequired(
            hidden = true,
            participant = false,
            submission = submission,
            ownTeamIds = setOf("team-own")
        )
        assertHidden(adminResult)
        assertEquals(ApiVerdictStatus.CORRECT, adminResult.answers.single().status)

        val endedTaskResult = HiddenResultsVisibility.redactIfRequired(
            hidden = false,
            participant = false,
            submission = submission,
            ownTeamIds = emptySet()
        )
        assertEquals("secret answer", endedTaskResult.answers.single().answers.single().text)
    }

    private fun assertHidden(submission: ApiSubmission) {
        val answer = submission.answers.single().answers.single()
        assertNull(answer.item)
        assertNull(answer.start)
        assertNull(answer.end)
        assertNull(answer.text)
        assertEquals(true, answer.hidden)
    }
}
