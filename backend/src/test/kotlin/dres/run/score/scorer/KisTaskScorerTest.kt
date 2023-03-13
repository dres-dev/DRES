package dres.run.score.scorer

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.evaluation.*
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.KisTaskScorer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KisTaskScorerTest {

    private lateinit var scorer: KisTaskScorer
    private val teams: List<TeamId> = listOf("team-1", "team-2", "team-3")
    private val dummyImageItem = ApiMediaItem("image1", "image 1", ApiMediaType.IMAGE, "testcollection", "images/1")
    private val defaultTaskDuration = 5 * 60L
    private  val taskStartTime = System.currentTimeMillis() - 100_000
    private val maxPointsPerTask = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 10.0
    private val scoreable = object: Scoreable {
        override val taskId: TaskId = "task1"
        override val teams: List<TeamId> = this@KisTaskScorerTest.teams
        override val duration: Long = this@KisTaskScorerTest.defaultTaskDuration
        override val started: Long = this@KisTaskScorerTest.taskStartTime
        override val ended: Long? = null
    }

    private val wrongAnswer = listOf(
        ApiAnswerSet(
            "wrong",
            ApiVerdictStatus.WRONG,
            "task",
            listOf(
                ApiAnswer(
                    ApiAnswerType.ITEM,
                    dummyImageItem,
                    null, null, null
                )
            )
        )
    )

    private val correctAnswer = listOf(
        ApiAnswerSet(
            "correct",
            ApiVerdictStatus.CORRECT,
            "task",
            listOf(
                ApiAnswer(
                    ApiAnswerType.ITEM,
                    dummyImageItem,
                    null, null, null
                )
            )
        )
    )

    @BeforeEach
    fun setup() {
        this.scorer = KisTaskScorer(this.scoreable, maxPointsPerTask, maxPointsAtTaskEnd, penaltyPerWrongSubmission)
    }

    @Test
    fun noSubmissions() {
        val scores = this.scorer.scoreMap(emptySequence())
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrong() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", wrongAnswer, taskStartTime + 1000, "task2"),
            ApiSubmission(teams[1], teams[1], "user2", "team1", "user2", wrongAnswer, taskStartTime + 2000, "task2"),
            ApiSubmission(teams[2], teams[2], "user3", "team1", "user3", wrongAnswer, taskStartTime + 3000, "task2")
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun immediatelyRight() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", correctAnswer, taskStartTime, "task3"),
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(maxPointsPerTask, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun rightAtTheEnd() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", correctAnswer, taskStartTime + (defaultTaskDuration * 1000), "task4"),
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(maxPointsAtTaskEnd, scores[teams[0]])
    }

    @Test
    fun rightInTheMiddle() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1",  "user1", correctAnswer, taskStartTime + (defaultTaskDuration * 1000 / 2), "task5"),
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2, scores[teams[0]])
    }

    @Test
    fun wrongSubmissionPenalty() {
        val submissions = sequenceOf(

            //incorrect submissions
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", wrongAnswer, taskStartTime + 1, "task6"),

            ApiSubmission(teams[1], teams[1], "user2", "team2","user2", wrongAnswer, taskStartTime + 2, "task6"),
            ApiSubmission(teams[1], teams[1], "user2", "team2","user2", wrongAnswer, taskStartTime + 3, "task6"),

            ApiSubmission(teams[2], teams[2], "user3", "team3","user3", wrongAnswer, taskStartTime + 4, "task6"),
            ApiSubmission(teams[2], teams[2], "user3", "team3","user3", wrongAnswer, taskStartTime + 5, "task6"),
            ApiSubmission(teams[2], teams[2], "user3", "team3","user3", wrongAnswer, taskStartTime + 6, "task6"),

            //correct submissions at 1/2 the task time
            ApiSubmission(teams[0], teams[0], "user1", "team1","user1", correctAnswer, taskStartTime + (defaultTaskDuration * 1000 / 2), "task6"),
            ApiSubmission(teams[1], teams[1], "user2", "team2","user2", correctAnswer, taskStartTime + (defaultTaskDuration * 1000 / 2), "task6"),
            ApiSubmission(teams[2], teams[2], "user3", "team3","user3", correctAnswer, taskStartTime + (defaultTaskDuration * 1000 / 2), "task6"),

        )
        val scores = this.scorer.scoreMap(submissions)

        assertEquals(
            maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission,
            scores[teams[0]]
        )
        assertEquals(
            maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission * 2,
            scores[teams[1]]
        )
        assertEquals(
            maxPointsAtTaskEnd + (maxPointsPerTask - maxPointsAtTaskEnd) / 2 - penaltyPerWrongSubmission * 3,
            scores[teams[2]]
        )
    }

}