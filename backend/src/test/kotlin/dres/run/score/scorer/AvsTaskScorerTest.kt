package dres.run.score.scorer

import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.run.ItemSubmission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.scorer.AvsTaskScorer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class AvsTaskScorerTest {

    private lateinit var scorer: AvsTaskScorer
    private val teams = listOf(UID(), UID(), UID()) //3 random team ids
    private val defaultTaskDuration = 5 * 60L
    private val dummyImageItems: List<MediaItem.ImageItem>
    private val dummyVideoItems: List<MediaItem.VideoItem>

    init {
        val collectionId = UID()

        dummyImageItems = listOf(
            MediaItem.ImageItem(UID(), "Image 1", "images/1", collectionId),
            MediaItem.ImageItem(UID(), "Image 2", "images/2", collectionId),
            MediaItem.ImageItem(UID(), "Image 3", "images/3", collectionId),
            MediaItem.ImageItem(UID(), "Image 4", "images/4", collectionId),
            MediaItem.ImageItem(UID(), "Image 5", "images/5", collectionId),
            MediaItem.ImageItem(UID(), "Image 6", "images/6", collectionId)
        )

        dummyVideoItems = listOf(
            MediaItem.VideoItem(UID(), "Video 1", "video/1", collectionId, 10 * 60 * 1000, 24f),
            MediaItem.VideoItem(UID(), "Video 2", "video/2", collectionId, 20 * 60 * 1000, 24f),
            MediaItem.VideoItem(UID(), "Video 3", "video/3", collectionId, 30 * 60 * 1000, 24f),
            MediaItem.VideoItem(UID(), "Video 4", "video/4", collectionId, 40 * 60 * 1000, 24f),
            MediaItem.VideoItem(UID(), "Video 5", "video/5", collectionId, 50 * 60 * 1000, 24f)
        )

    }

    @BeforeEach
    fun setup() {
        this.scorer = AvsTaskScorer()
    }

    @Test
    fun noSubmissions() {
        val scores = this.scorer.computeScores(emptyList(), teams, 100_000, defaultTaskDuration)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrongSameImage() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
            ItemSubmission(teams[0], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
            ItemSubmission(teams[1], UID(), taskStartTime + 2000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
            ItemSubmission(teams[2], UID(), taskStartTime + 3000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG }
        )
        val scores = this.scorer.computeScores(submissions, teams, taskStartTime, defaultTaskDuration)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allWrongDifferentImages() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
            ItemSubmission(teams[0], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.WRONG },
            ItemSubmission(teams[1], UID(), taskStartTime + 2000, dummyImageItems[1]).also { it.status = SubmissionStatus.WRONG },
            ItemSubmission(teams[2], UID(), taskStartTime + 3000, dummyImageItems[2]).also { it.status = SubmissionStatus.WRONG }
        )
        val scores = this.scorer.computeScores(submissions, teams, taskStartTime, defaultTaskDuration)
        assertEquals(0.0, scores[teams[0]])
        assertEquals(0.0, scores[teams[1]])
        assertEquals(0.0, scores[teams[2]])
    }

    @Test
    fun allSameImageAllCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
            ItemSubmission(teams[0], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[1], UID(), taskStartTime + 2000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[2], UID(), taskStartTime + 3000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT }
        )
        val scores = this.scorer.computeScores(submissions, teams, taskStartTime, defaultTaskDuration)
        assertEquals(100.0, scores[teams[0]])
        assertEquals(100.0, scores[teams[1]])
        assertEquals(100.0, scores[teams[2]])
    }

    @Test
    fun allDifferentImageAllCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(
            ItemSubmission(teams[0], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[1], UID(), taskStartTime + 2000, dummyImageItems[1]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[2], UID(), taskStartTime + 3000, dummyImageItems[2]).also { it.status = SubmissionStatus.CORRECT }
        )
        val scores = this.scorer.computeScores(submissions, teams, taskStartTime, defaultTaskDuration)
        assertEquals(100.0 / 3.0, scores[teams[0]]!!, 0.001)
        assertEquals(100.0 / 3.0, scores[teams[1]]!!, 0.001)
        assertEquals(100.0 / 3.0, scores[teams[2]]!!, 0.001)
    }

    @Test
    fun someDifferentImageSomeCorrect() {
        val taskStartTime = System.currentTimeMillis() - 100_000
        val submissions = listOf(

            /*
            total correct: 4 (0, 1, 2, 3)
            total wrong: 2 (4, 5)
             */

            //3 out of 4 correct
            ItemSubmission(teams[0], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[0], UID(), taskStartTime + 2000, dummyImageItems[1]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[0], UID(), taskStartTime + 3000, dummyImageItems[2]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[0], UID(), taskStartTime + 4000, dummyImageItems[4]).also { it.status = SubmissionStatus.WRONG },


            //1 out of 3 correct
            ItemSubmission(teams[1], UID(), taskStartTime + 1000, dummyImageItems[0]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[1], UID(), taskStartTime + 2000, dummyImageItems[5]).also { it.status = SubmissionStatus.WRONG },
            ItemSubmission(teams[1], UID(), taskStartTime + 3000, dummyImageItems[4]).also { it.status = SubmissionStatus.WRONG },

            //1 out of 2 correct
            ItemSubmission(teams[2], UID(), taskStartTime + 2000, dummyImageItems[3]).also { it.status = SubmissionStatus.CORRECT },
            ItemSubmission(teams[2], UID(), taskStartTime + 3000, dummyImageItems[5]).also { it.status = SubmissionStatus.WRONG }
        )


        val scores = this.scorer.computeScores(submissions, teams, taskStartTime, defaultTaskDuration)

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


}