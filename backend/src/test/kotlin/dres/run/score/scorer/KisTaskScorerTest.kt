package dres.run.score.scorer

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.scorer.KisTaskScorer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KisTaskScorerTest {

    private lateinit var scorer: KisTaskScorer
    private val teams = listOf(EvaluationId(), EvaluationId(), EvaluationId()) //3 random team ids
    private val dummyImageItems = listOf(MediaItem.ImageItem(EvaluationId(), "image 1", "images/1", EvaluationId()))
    private val defaultTaskDuration = 5 * 60L
    private val maxPointsPerTask = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 10.0

    @BeforeEach
    fun setup() {
        this.scorer = KisTaskScorer(maxPointsPerTask, maxPointsAtTaskEnd, penaltyPerWrongSubmission)
    }

    @Test
    fun noSubmissions() {
        val scores = this.scorer.computeScores(emptyList(), TaskContext(teams, 100_000, defaultTaskDuration))
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrong() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
                Submission.Item(teams[0], EvaluationId(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
                Submission.Item(teams[1], EvaluationId(), taskStartTime + 2000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
                Submission.Item(teams[2], EvaluationId(), taskStartTime + 3000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG }
        )
        val scores = this.scorer.computeScores(submissions, TaskContext(teams, taskStartTime, defaultTaskDuration))
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun immediatelyRight() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
                Submission.Item(teams[0], EvaluationId(), taskStartTime, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT }
        )
        val scores = this.scorer.computeScores(submissions, TaskContext(teams, taskStartTime, defaultTaskDuration))
        assertEquals(maxPointsPerTask, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun rightAtTheEnd() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
                Submission.Item(teams[0], EvaluationId(), taskStartTime + (defaultTaskDuration * 1000), dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT }
        )
        val scores = this.scorer.computeScores(submissions, TaskContext(teams, taskStartTime, defaultTaskDuration))
        assertEquals(maxPointsAtTaskEnd, scores[teams[0]])
    }

    @Test
    fun rightInTheMiddle() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
                Submission.Item(teams[0], EvaluationId(), taskStartTime + (defaultTaskDuration * 1000 / 2), dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT }
        )
        val scores = this.scorer.computeScores(submissions, TaskContext(teams, taskStartTime, defaultTaskDuration))
        assertEquals(maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2, scores[teams[0]])
    }

    @Test
    fun wrongSubmissionPenalty() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(

                //incorrect submissions
                Submission.Item(teams[0], EvaluationId(), taskStartTime + 1, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },

                Submission.Item(teams[1], EvaluationId(), taskStartTime + 2, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
                Submission.Item(teams[1], EvaluationId(), taskStartTime + 3, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },

                Submission.Item(teams[2], EvaluationId(), taskStartTime + 4, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
                Submission.Item(teams[2], EvaluationId(), taskStartTime + 5, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
                Submission.Item(teams[2], EvaluationId(), taskStartTime + 6, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },

                //correct submissions at 1/2 the task time
                Submission.Item(teams[0], EvaluationId(), taskStartTime + (defaultTaskDuration * 1000 / 2), dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
                Submission.Item(teams[1], EvaluationId(), taskStartTime + (defaultTaskDuration * 1000 / 2), dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
                Submission.Item(teams[2], EvaluationId(), taskStartTime + (defaultTaskDuration * 1000 / 2), dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
        )
        val scores = this.scorer.computeScores(submissions, TaskContext(teams, taskStartTime, defaultTaskDuration))

        assertEquals(maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission, scores[teams[0]])
        assertEquals(maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission * 2, scores[teams[1]])
        assertEquals(maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission * 3, scores[teams[2]])
    }

}