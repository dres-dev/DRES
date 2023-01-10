package dres.run.score.scorer

import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.scorer.NewAvsTaskScorer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NewAvsTaskScorerTest {

    private lateinit var scorer: NewAvsTaskScorer
    private val teams = listOf(UID(),UID(),UID()) // 3 teams
    private val defaultTaskDuration = 3 * 60L // 3min
    private val dummyVideoItems: List<MediaItem.VideoItem>
    private val maxPointsPerTask = 1000.0
    private val penalty = 0.2

    init {
        val collectionId = UID()
        val list = mutableListOf<MediaItem.VideoItem>()
        for (i in 1..10){
            list.add(MediaItem.VideoItem(UID(), "Video $i", "videos/$i", collectionId, i*10*60*1000L,24f))
        }
        dummyVideoItems = list
    }

    @BeforeEach
    fun setup(){
        this.scorer = NewAvsTaskScorer(penaltyConstant = penalty, maxPointsPerTask)
    }

    @Test
    @DisplayName("Three teams all without a submission. Expected score: 0.0")
    fun testNoSubmissions(){
        val scores = this.scorer.computeScores(emptyList(), TaskContext(teams, 100_000, defaultTaskDuration))

        Assertions.assertEquals(0.0, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with a single correct submission. Expected score: 1000 (maxPointsPerTask)")
    fun testOnlyTeamOneWithAllEqualsOneCorrect(){
        val taskStart = 100_000L
        val subs = listOf(
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT}
        )
        val scores = this.scorer.computeScores(subs, TaskContext(teams, taskStart, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("All teams with exact same, correct submission. Expected score: 1000 each")
    fun testAllTeamsWithAllEuqalsOneCorrect(){
        val taskStart = 100_000L
        val subs = listOf(
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[1], UID(), taskStart+2000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[2], UID(), taskStart+3000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT}
        )
        val scores = this.scorer.computeScores(subs, TaskContext(teams, taskStart, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[1]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 2 / 2 correct videos, Team Two with 1 / 2 correct videos, Team Three without submission")
    fun testTeamsWithVariousSubmissionsTwoOfTwoAndOneOfTwoAndNoneOfTwo(){
        val taskStart = 100_000L
        val subs = listOf(
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[0], UID(), taskStart+2000, dummyVideoItems[1], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[1], UID(), taskStart+3000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT}
        )
        val scores = this.scorer.computeScores(subs, TaskContext(teams, taskStart, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask/2.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 3/3 correct videos. Team Two with 2/3 correct (and one on the second attempt), Team Three with Brute Force (0 wrong, 1 wrong and 2 wrong")
    fun testTeamsWithVariousSubmissionsTeamOneAllTeamTwoOneWrongTeamThreeBruteForce(){
        val taskStart = 100_000L
        val subs = listOf(
            /* Team One: All correct */
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[0], UID(), taskStart+2000, dummyVideoItems[1], 20_000, 30_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[0], UID(), taskStart+3000, dummyVideoItems[2], 30_000, 40_000).also{it.status = SubmissionStatus.CORRECT},
            /* Team Two: One correct, One correct with one wrong */
            Submission.Temporal(teams[1], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[1], UID(), taskStart+2000, dummyVideoItems[1], 10_000, 20_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[1], UID(), taskStart+3000, dummyVideoItems[1], 30_000, 40_000).also{it.status = SubmissionStatus.CORRECT},
            /* Team Three: Brute Force: (correct/wrong): v1: (1/0), v2: (1/1), v3: (1/2), v4: (0/3), v5: (0/3)*/
            /* v1 */
            Submission.Temporal(teams[2], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            /* v2 */
            Submission.Temporal(teams[2], UID(), taskStart+2000, dummyVideoItems[1], 10_000, 20_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+3000, dummyVideoItems[1], 30_000, 40_000).also{it.status = SubmissionStatus.CORRECT},
            /* v3 */
            Submission.Temporal(teams[2], UID(), taskStart+4000, dummyVideoItems[2], 10_000, 20_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+5000, dummyVideoItems[2], 20_000, 30_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+6000, dummyVideoItems[2], 30_000, 40_000).also{it.status = SubmissionStatus.CORRECT},
            /* v4 */
            Submission.Temporal(teams[2], UID(), taskStart+7000, dummyVideoItems[3], 10_000, 20_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+8000, dummyVideoItems[3], 20_000, 30_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+9000, dummyVideoItems[3], 30_000, 40_000).also{it.status = SubmissionStatus.WRONG},
            /* v5 */
            Submission.Temporal(teams[2], UID(), taskStart+10_000, dummyVideoItems[4], 10_000, 20_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+11_000, dummyVideoItems[4], 20_000, 30_000).also{it.status = SubmissionStatus.WRONG},
            Submission.Temporal(teams[2], UID(), taskStart+12_000, dummyVideoItems[4], 30_000, 40_000).also{it.status = SubmissionStatus.WRONG},
        )
        val scores = this.scorer.computeScores(subs, TaskContext(teams, taskStart, defaultTaskDuration))
        /*
        Team One: No penalty => 1000 = 1000 * [1/3 * 1+1+1] => 1000 * 3/3
         */
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        /*
        Team Two: One penalty => 600 = 1000 * [1/3 * (1 + 1-0.2)] => 1000 * 1.8/3
         */
        Assertions.assertEquals(600.0, scores[teams[1]]!!, 0.001)
        /*
        Team Three: Lucky Brute Force => 400 = 1000 * [1/3 * (1 + {1-0.2} + {1-0.4} + {-3*0.2} + {-3*0.2})] => 1000 * 1.2/3
         */
        Assertions.assertEquals(400.0, scores[teams[2]]!!, 0.001)
    }

    @Test()
    @DisplayName("Only wrong and correct submissions are considered")
    fun testOnlyCorrectAndWrongSubmissionsAreConsidered(){
        val taskStart = 100_000L
        val subs = listOf(
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.CORRECT},
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.INDETERMINATE},
            Submission.Temporal(teams[0], UID(), taskStart+1000, dummyVideoItems[0], 10_000, 20_000).also{it.status = SubmissionStatus.INDETERMINATE}
        )
        val scores = this.scorer.computeScores(subs, TaskContext(teams, taskStart, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }
}
