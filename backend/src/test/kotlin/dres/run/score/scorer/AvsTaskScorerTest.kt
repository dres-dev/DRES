package dres.run.score.scorer


import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.evaluation.*
import dev.dres.api.rest.types.evaluation.submission.*
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.AvsTaskScorer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AvsTaskScorerTest {

    private lateinit var scorer: AvsTaskScorer
    private val teams = listOf("team-1", "team-2", "team-3")
    private val defaultTaskDuration = 3 * 60L // 3min
    private val taskStartTime = 100_000L
    private val dummyVideoItems: List<ApiMediaItem>
    private val maxPointsPerTask = 1000.0
    private val penalty = 0.2
    private val scoreable = object : Scoreable {
        override val taskId: TaskId = "task1"
        override val teams: List<TeamId> = this@AvsTaskScorerTest.teams
        override val duration: Long = this@AvsTaskScorerTest.defaultTaskDuration
        override val started: Long = this@AvsTaskScorerTest.taskStartTime
        override val ended: Long? = null
    }

    init {
        val collectionId = "testCollection"
        val list = mutableListOf<ApiMediaItem>()
        for (i in 1..10) {
            list.add(
                ApiMediaItem(
                    "video$i",
                    "video $i",
                    ApiMediaType.VIDEO,
                    collectionId,
                    "videos/$i",
                    10 * 60 * 1000 * i.toLong(),
                    24f
                )
            )
        }
        dummyVideoItems = list
    }

    private fun answerSets(status: ApiVerdictStatus, item: ApiMediaItem, start: Long, end: Long, taskId: String) =
        listOf(
            ApiAnswerSet(
                "dummyId",
                status,
                taskId,
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
        this.scorer = AvsTaskScorer(this.scoreable, penaltyConstant = penalty, maxPointsPerTask)
    }

    @Test
    @DisplayName("Three teams all without a submission. Expected score: 0.0")
    fun noSubmissions() {
        val scores = this.scorer.scoreMap(emptySequence())
        Assertions.assertEquals(0.0, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with a single correct submission. Expected score: 1000 (maxPointsPerTask)")
    fun onlyTeamOneWithAllEqualsOneCorrect() {
        val subs = sequenceOf(
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2")
            ),
        )
        val scores = this.scorer.scoreMap(subs)
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("All teams with exact same, correct submission. Expected score: 1000 each")
    fun allTeamsWithAllEqualsOneCorrect() {
        val subs = sequenceOf(
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2")
            ),
            ApiSubmission(
                teams[1],
                teams[1],
                "user2",
                "team2",
                "user2",
                taskStartTime + 2000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 3000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2")
            )
        )
        val scores = this.scorer.scoreMap(subs)
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[1]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 2 / 2 correct videos, Team Two with 1 / 2 correct videos, Team Three without submission")
    fun teamsWithVariousSubmissionsTwoOfTwoAndOneOfTwoAndNoneOfTwo() {
        val subs = sequenceOf(
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task3")
            ),
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team2",
                "user1",
                taskStartTime + 2000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 10_000, 20_000, "task3")
            ),
            ApiSubmission(
                teams[1],
                teams[1],
                "user2",
                "team3",
                "user2",
                taskStartTime + 3000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task3")
            ),
        )
        val scores = this.scorer.scoreMap(subs)
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask / 2.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 3/3 correct videos. Team Two with 2/3 correct (and one on the second attempt), Team Three with Brute Force (0 wrong, 1 wrong and 2 wrong")
    fun teamsWithVariousSubmissionsTeamOneAllTeamTwoOneWrongTeamThreeBruteForce() {
        val subs = sequenceOf(
            /* Team One: All correct */
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 2000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 20_000, 30_000, "task4")
            ),
            ApiSubmission(
                teams[0],
                teams[0],
                "user1",
                "team1",
                "user1",
                taskStartTime + 3000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 30_000, 40_000, "task4")
            ),

            /* Team Two: One correct, One correct with one wrong */
            ApiSubmission(
                teams[1],
                teams[1],
                "user2",
                "team2",
                "user2",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[1],
                teams[1],
                "user2",
                "team2",
                "user2",
                taskStartTime + 2000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[1], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[1],
                teams[1],
                "user2",
                "team2",
                "user2",
                taskStartTime + 3000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 30_000, 40_000, "task4")
            ),

            /* Team Three: Brute Force: (correct/wrong): v1: (1/0), v2: (1/1), v3: (1/2), v4: (0/3), v5: (0/3)*/
            /* v1 */
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 1000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4")
            ),
            /* v2 */
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 2000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[1], 10_000, 20_000, "task4"),
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 3000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 30_000, 40_000, "task4")
            ),
            /* v3 */
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 4000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[2], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 5000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[2], 20_000, 30_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 6000,
                answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 30_000, 40_000, "task4")
            ),
            /* v4 */
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 7000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 8000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 20_000, 30_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 9000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 30_000, 40_000, "task4")
            ),
            /* v5 */
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 10000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 10_000, 20_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 11000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 20_000, 30_000, "task4")
            ),
            ApiSubmission(
                teams[2],
                teams[2],
                "user3",
                "team3",
                "user3",
                taskStartTime + 12000,
                answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 30_000, 40_000, "task4")
            ),

            )
        val scores = this.scorer.scoreMap(subs)
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
}
