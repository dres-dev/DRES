package dres.run.score.scorer


import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.evaluation.*
import dev.dres.run.score.TaskContext
import dev.dres.run.score.scorer.NewAvsTaskScorer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AvsTaskScorerTest {

    private lateinit var scorer: NewAvsTaskScorer
    private val teams = listOf("team-1", "team-2", "team-3")
    private val defaultTaskDuration = 3 * 60L // 3min
    private val dummyVideoItems: List<ApiMediaItem>
    private val maxPointsPerTask = 1000.0
    private val penalty = 0.2

    init {
        val collectionId = "testCollection"
        val list = mutableListOf<ApiMediaItem>()
        for (i in 1..10){
            list.add(ApiMediaItem("video$i", "video $i", ApiMediaType.VIDEO, collectionId, "videos/$i", 10 * 60 * 1000 * i.toLong(), 24f))
        }
        dummyVideoItems = list
    }

    private fun answerSets(status: ApiVerdictStatus, item: ApiMediaItem, start: Long, end: Long, taskId: String) = listOf(
        ApiAnswerSet(
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
    fun setup(){
        this.scorer = NewAvsTaskScorer(penaltyConstant = penalty, maxPointsPerTask)
    }

    @Test
    @DisplayName("Three teams all without a submission. Expected score: 0.0")
    fun noSubmissions(){
        val scores = this.scorer.computeScores(emptySequence(), TaskContext("task1", teams, 100_000, defaultTaskDuration))

        Assertions.assertEquals(0.0, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with a single correct submission. Expected score: 1000 (maxPointsPerTask)")
    fun onlyTeamOneWithAllEqualsOneCorrect(){
        val taskStart = 100_000L
        val subs = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2"), taskStart + 1000, "task2" ),
        )
        val scores = this.scorer.computeScores(subs, TaskContext("task2", teams, 100_000, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(0.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("All teams with exact same, correct submission. Expected score: 1000 each")
    fun allTeamsWithAllEqualsOneCorrect(){
        val taskStart = 100_000L
        val subs = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2"), taskStart + 1000, "task2"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2"), taskStart + 2000, "task2"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task2"), taskStart + 3000, "task2")
        )
        val scores = this.scorer.computeScores(subs, TaskContext("task2", teams, 100_000, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[1]])
        Assertions.assertEquals(maxPointsPerTask, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 2 / 2 correct videos, Team Two with 1 / 2 correct videos, Team Three without submission")
    fun teamsWithVariousSubmissionsTwoOfTwoAndOneOfTwoAndNoneOfTwo(){
        val taskStart = 100_000L
        val subs = sequenceOf(
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task3"), taskStart + 1000, "task3"),
            ApiSubmission(teams[0], teams[0], "user1", "team2", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 10_000, 20_000, "task3"), taskStart + 2000, "task3"),
            ApiSubmission(teams[1], teams[1], "user2", "team3", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task3"), taskStart + 3000, "task3"),
        )
        val scores = this.scorer.computeScores(subs, TaskContext("task3", teams, 100_000, defaultTaskDuration))
        Assertions.assertEquals(maxPointsPerTask, scores[teams[0]])
        Assertions.assertEquals(maxPointsPerTask/2.0, scores[teams[1]])
        Assertions.assertEquals(0.0, scores[teams[2]])
    }

    @Test
    @DisplayName("Team One with 3/3 correct videos. Team Two with 2/3 correct (and one on the second attempt), Team Three with Brute Force (0 wrong, 1 wrong and 2 wrong")
    fun teamsWithVariousSubmissionsTeamOneAllTeamTwoOneWrongTeamThreeBruteForce(){
        val taskStart = 100_000L
        val subs = sequenceOf(
            /* Team One: All correct */
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4"), taskStart + 1000, "task4"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 20_000, 30_000, "task4"), taskStart + 2000, "task4"),
            ApiSubmission(teams[0], teams[0], "user1", "team1", "user1", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 30_000, 40_000, "task4"), taskStart + 3000, "task4"),

            /* Team Two: One correct, One correct with one wrong */
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4"), taskStart + 1000, "task4"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[1], 10_000, 20_000, "task4"), taskStart + 2000, "task4"),
            ApiSubmission(teams[1], teams[1], "user2", "team2", "user2", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 30_000, 40_000, "task4"), taskStart + 3000, "task4"),

            /* Team Three: Brute Force: (correct/wrong): v1: (1/0), v2: (1/1), v3: (1/2), v4: (0/3), v5: (0/3)*/
            /* v1 */
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[0], 10_000, 20_000, "task4"), taskStart + 1000, "task4"),
            /* v2 */
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[1], 10_000, 20_000, "task4"), taskStart + 2000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[1], 30_000, 40_000, "task4"), taskStart + 3000, "task4"),
            /* v3 */
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[2], 10_000, 20_000, "task4"), taskStart + 4000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[2], 20_000, 30_000, "task4"), taskStart + 5000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.CORRECT, dummyVideoItems[2], 30_000, 40_000, "task4"), taskStart + 6000, "task4"),
            /* v4 */
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 10_000, 20_000, "task4"), taskStart + 7000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 20_000, 30_000, "task4"), taskStart + 8000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[3], 30_000, 40_000, "task4"), taskStart + 9000, "task4"),
            /* v5 */
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 10_000, 20_000, "task4"), taskStart + 10000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 20_000, 30_000, "task4"), taskStart + 11000, "task4"),
            ApiSubmission(teams[2], teams[2], "user3", "team3", "user3", answerSets(ApiVerdictStatus.WRONG, dummyVideoItems[4], 30_000, 40_000, "task4"), taskStart + 12000, "task4"),

            )
        val scores = this.scorer.computeScores(subs, TaskContext("task4", teams, 100_000, defaultTaskDuration))
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
