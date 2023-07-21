package dres.run.score.scorer

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.evaluation.*
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.LegacyAvsTaskScorer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class LegacyAvsTaskScorerTest {

    private lateinit var scorer: LegacyAvsTaskScorer
    private val teams = listOf("team-1", "team-2", "team-3")
    private val defaultTaskDuration = 5 * 60L
    private val taskStartTime =  System.currentTimeMillis() - 100_000
    private val dummyImageItems: List<ApiMediaItem>
    private val dummyVideoItems: List<ApiMediaItem>
    private val scoreable = object: Scoreable {
        override val taskId: TaskId = "task1"
        override val teams: List<TeamId> = this@LegacyAvsTaskScorerTest.teams
        override val duration: Long = this@LegacyAvsTaskScorerTest.defaultTaskDuration
        override val started: Long = this@LegacyAvsTaskScorerTest.taskStartTime
        override val ended: Long? = null
    }

    init {
        val collectionId = "testCollection"

        dummyImageItems = listOf(
            ApiMediaItem("image1", "image 1", ApiMediaType.IMAGE, collectionId, "images/1"),
            ApiMediaItem("image2", "image 2", ApiMediaType.IMAGE, collectionId, "images/2"),
            ApiMediaItem("image3", "image 3", ApiMediaType.IMAGE, collectionId, "images/3"),
            ApiMediaItem("image4", "image 4", ApiMediaType.IMAGE, collectionId, "images/4"),
            ApiMediaItem("image5", "image 5", ApiMediaType.IMAGE, collectionId, "images/5"),
            ApiMediaItem("image6", "image 6", ApiMediaType.IMAGE, collectionId, "images/6")
        )

        dummyVideoItems = listOf(
            ApiMediaItem("video1", "video 1", ApiMediaType.VIDEO, collectionId, "videos/1", 10 * 60 * 1000, 24f),
            ApiMediaItem("video2", "video 2", ApiMediaType.VIDEO, collectionId, "videos/2", 10 * 60 * 1000, 24f),
            ApiMediaItem("video3", "video 3", ApiMediaType.VIDEO, collectionId, "videos/3", 10 * 60 * 1000, 24f),
            ApiMediaItem("video4", "video 4", ApiMediaType.VIDEO, collectionId, "videos/4", 10 * 60 * 1000, 24f),
            ApiMediaItem("video5", "video 5", ApiMediaType.VIDEO, collectionId, "videos/5", 10 * 60 * 1000, 24f),
            ApiMediaItem("video6", "video 6", ApiMediaType.VIDEO, collectionId, "videos/6", 10 * 60 * 1000, 24f),
        )

    }

    private fun answerSets(status: ApiVerdictStatus, item: ApiMediaItem) = listOf(
        ApiAnswerSet(
            "dummyId",
            status,
            "task",
            listOf(
                ApiAnswer(
                    ApiAnswerType.ITEM,
                    item,
                    null, null, null
                )
            )
        )
    )

    private fun answerSets(status: ApiVerdictStatus, item: ApiMediaItem, start: Long, end: Long) = listOf(
        ApiAnswerSet(
            "",
            status,
            "task",
            listOf(
                ApiAnswer(
                    ApiAnswerType.TEMPORAL,
                    item,
                    null,
                    start,
                    end
                )
            )
        )
    )

    @BeforeEach
    fun setup() {
        this.scorer = LegacyAvsTaskScorer(this.scoreable)
    }

    @Test
    fun noSubmissions() {
        val scores = this.scorer.scoreMap(emptySequence())
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrongSameImage() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[0]), taskStartTime + 1000, "task2"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[0]), taskStartTime + 2000, "task2"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[0]), taskStartTime + 3000, "task2")
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrongDifferentImages() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[0]), taskStartTime + 1000, "task3"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[1]), taskStartTime + 2000, "task3"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[2]), taskStartTime + 3000, "task3")
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allSameImageAllCorrect() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 1000, "task4"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 2000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 3000, "task4")
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(100.0, scores[teams[0]])
        assertEquals(100.0, scores[teams[1]])
        assertEquals(100.0, scores[teams[2]])
    }

    @Test
    fun allDifferentImageAllCorrect() {
        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 1000, "task5"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[1]), taskStartTime + 2000, "task5"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[2]), taskStartTime + 3000, "task5")
        )
        val scores = this.scorer.scoreMap(submissions)
        assertEquals(100.0 / 3.0, scores[teams[0]]!!, 0.001)
        assertEquals(100.0 / 3.0, scores[teams[1]]!!, 0.001)
        assertEquals(100.0 / 3.0, scores[teams[2]]!!, 0.001)
    }

    @Test
    fun someDifferentImageSomeCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = sequenceOf(

            /*
            total correct: 4 (0, 1, 2, 3)
            total wrong: 2 (4, 5)
             */

            //3 out of 4 correct
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 1000, "task6"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[1]), taskStartTime + 2000, "task6"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[2]), taskStartTime + 3000, "task6"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[3]), taskStartTime + 4000, "task6"),

            //1 out of 3 correct
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[0]), taskStartTime + 1000, "task6"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[4]), taskStartTime + 2000, "task6"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[5]), taskStartTime + 3000, "task6"),

            //1 out of 2 correct
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyImageItems[3]), taskStartTime + 2000, "task6"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyImageItems[5]), taskStartTime + 3000, "task6"),
        )


        val scores = this.scorer.scoreMap(submissions)

        /*
        c = q(c) = 3, i = 1, q(p) = 4

        100 * 3   3
        ------- * - = 64.28571428571429
        3 + 1/2   4
         */
        assertEquals(64.28571428571429, scores[teams[0]]!!, 0.001)

        /*
        c = q(c) = 1, i = 1, q(p) = 4

        100 * 1   1
        ------- * - = 12.5
        1 + 2/2   4
         */
        assertEquals(12.5, scores[teams[1]]!!, 0.001)

        /*
        c = q(c) = 3, i = 1, q(p) = 4

        100 * 1   1
        ------- * - = 16.66666666666667
        1 + 1/2   4
         */
        assertEquals(16.66666666666667, scores[teams[2]]!!, 0.001)
    }


    @Test
    fun allSameVideoSameSegmentAllCorrect() {

        val taskStartTime = System.currentTimeMillis() - 100_000

        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task7"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 2000, "task7"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 3000, "task7")
        )

        val scores = this.scorer.scoreMap(submissions)
        assertEquals(100.0, scores[teams[0]])
        assertEquals(100.0, scores[teams[1]])
        assertEquals(100.0, scores[teams[2]])
    }


    @Test
    fun allSameVideoDifferentSegmentAllCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000

        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task8"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 30_000, 40_000), taskStartTime + 2000, "task8"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 50_000, 60_000), taskStartTime + 3000, "task8")
        )

        val scores = this.scorer.scoreMap(submissions)
        assertEquals(33.33333333333333, scores[teams[0]]!!, 0.0001)
        assertEquals(33.33333333333333, scores[teams[1]]!!, 0.0001)
        assertEquals(33.33333333333333, scores[teams[2]]!!, 0.0001)
    }


    @Test
    fun allDifferentVideoDifferentSegmentAllCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000

        val submissions = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task9"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 30_000, 40_000), taskStartTime + 2000, "task9"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[3], 50_000, 60_000), taskStartTime + 3000, "task9")
        )

        val scores = this.scorer.scoreMap(submissions)
        assertEquals(33.33333333333333, scores[teams[0]]!!, 0.0001)
        assertEquals(33.33333333333333, scores[teams[1]]!!, 0.0001)
        assertEquals(33.33333333333333, scores[teams[2]]!!, 0.0001)
    }

    @Test
    fun allSameVideoOverlappingSegmentAllCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000

        val submissions = sequenceOf(

            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task10"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 2000, "task10"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 3000, "task10"),

            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task10"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 2000, "task10"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 3000, "task10"),

            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task10"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 1000, "task10"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 1000, "task10"),
        )

        val scores = this.scorer.scoreMap(submissions)
        assertEquals(100.0, scores[teams[0]])
        assertEquals(100.0, scores[teams[1]])
        assertEquals(100.0, scores[teams[2]])
    }

    @Test
    fun allSameVideoOverlappingSegmentSomeCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = sequenceOf(

            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task11"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 2000, "task11"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 3000, "task11"),

            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task11"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 2000, "task11"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 3000, "task11"),

            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task11"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 11_000, 21_000), taskStartTime + 1000, "task11"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 12_000, 22_000), taskStartTime + 1000, "task11"),

            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 20_000), taskStartTime + 1000, "task11"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 20_000), taskStartTime + 2000, "task11"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 20_000), taskStartTime + 3000, "task11"),

        )
        val scores = this.scorer.scoreMap(submissions)

        assertEquals(85.71428571428571, scores[teams[0]]!!, 0.001)
        assertEquals(85.71428571428571, scores[teams[1]]!!, 0.001)
        assertEquals(85.71428571428571, scores[teams[2]]!!, 0.001)
    }


    @Test
    fun partiallyMergedSubmissionSomeCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = sequenceOf(
            //team 1
            //gets merged to one
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task12"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 20_001, 25_000), taskStartTime + 2000, "task12"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 25_001, 30_000), taskStartTime + 3000, "task12"),

            //plus 2 independent correct ones
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 5_000, 6_000), taskStartTime + 4000, "task12"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 3_000, 4_000), taskStartTime + 5000, "task12"),

            //and an incorrect one directly next to it
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[2], 4_001, 5_000), taskStartTime + 6000, "task12"),

            //c = 5, q(c) = 3, i = 1


            //team 2
            //the center part is missing, so it's counted as two in the quantization
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task12"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 25_001, 30_000), taskStartTime + 3000, "task12"),

            //another correct one, same as team 1
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 5_000, 6_000), taskStartTime + 4000, "task12"),

            //and two wrong ones
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 0, 5_000), taskStartTime + 6000, "task12"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 15_000), taskStartTime + 6000, "task12"),


            //c = 3, q(c) = 3, i = 2

            //team 3
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000), taskStartTime + 1000, "task12"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 20_001, 25_000), taskStartTime + 2000, "task12"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 25_001, 30_000), taskStartTime + 3000, "task12"),

            //another correct one, same as team 1
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 5_000, 6_000), taskStartTime + 4000, "task12"),


            //and two wrong ones
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 0, 5_000), taskStartTime + 4000, "task12"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 15_000), taskStartTime + 4000, "task12"),

            //c = 4, q(c) = 2, i = 2

        )
        val scores = this.scorer.scoreMap(submissions)

        /*
        c = 5, q(c) = 3, i = 1, q(p) = 3

        100 * 5   3
        ------- * - = 90.909090909090
        5 + 1/2   3
         */
        assertEquals(90.909090909090, scores[teams[0]]!!, 0.001)

        /*
        c = 3, q(c) = 3, i = 2, q(p) = 3

        100 * 3   3
        ------- * - = 75
        3 + 2/2   3
         */
        assertEquals(75.0, scores[teams[1]])

        /*
        c = 4, q(c) = 2, i = 2, q(p) = 3

        100 * 4   2
        ------- * - = 53.33333333333
        4 + 2/2   3
         */
        assertEquals(53.33333333333, scores[teams[2]]!!, 0.001)
    }

}
