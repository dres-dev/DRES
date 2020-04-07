package dres.run.score

import dres.data.model.competition.Task
import dres.data.model.competition.Team
import dres.data.model.run.CompetitionRun
import dres.data.model.run.SubmissionStatus
import kotlin.math.max

class KisScoreBoard(private val name: String, private val run: CompetitionRun, private val taskFilter: (Task) -> Boolean) : Scoreboard {

    private val maxPointsPerTask = 100.0
    private val maxScoreNormalized = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 20.0

    private val scorePerTaskMap = mutableMapOf<Task, Map<Team, Double>>()

    override fun taskScores(): List<Score> {

        val currentTask: Task = run.currentTask?.task ?: return emptyList()
        return scorePerTaskMap[currentTask]?.map { Score(run.competition.teams.indexOf(it.key), it.value) } ?: emptyList()

    }

    private fun overallScoreMap(): Map<Team, Double> {
        val scoreSums = scorePerTaskMap.values
                .flatMap {it.entries} //all team to score pairs independent of task
                .groupBy { it.key } //individual scores per team
                .mapValues {
                    it.value.map { i -> i.value }.sum()
                }

        val maxScore = scoreSums.values.max() ?: return emptyMap()

        return scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
    }

    override fun overallScores(): List<Score> {
        return overallScoreMap().entries.map {
            Score(run.competition.teams.indexOf(it.key), it.value)
        }
    }

    override fun taskScore(team: Team) = overallScoreMap()[team] ?: 0.0

    //TODO introduce some caching
    override fun update() {

        val submissions = run.runs.filter { it.started != null && taskFilter(it.task) }
                .associateWith { it.submissions.groupBy { it.team } }

        //recomputes the scores of all tasks

        val scoresPerTask = submissions.map {
            val taskStart = it.key.started!!

            //actual duration of task, in case it was extended during competition
            val taskDuration = max(it.key.task.duration, (it.key.ended ?: 0) - taskStart ).toDouble()

            it.key.task to it.value.map{

                //explicitly enforce valid types and order
                val sorted =  it.value.filter { it.status == SubmissionStatus.CORRECT || it.status == SubmissionStatus.WRONG }.sortedBy { it.timestamp }

                val firstCorrect = sorted.indexOfFirst { it.status == SubmissionStatus.CORRECT }

                val score = if (firstCorrect > -1) {
                    val incorrectSubmissions = firstCorrect + 1
                    val timeFraction = (sorted[firstCorrect].timestamp - taskStart) / taskDuration

                    max(0.0,
                            maxPointsAtTaskEnd +
                                    ((maxPointsPerTask - maxPointsAtTaskEnd) * timeFraction) -
                                    (incorrectSubmissions * penaltyPerWrongSubmission)
                    )
                } else {
                    0.0
                }
                run.competition.teams[it.key] to score

            }.toMap()
        }.toMap()

        scorePerTaskMap.clear()
        scorePerTaskMap.putAll(scoresPerTask)
    }

    override fun name() = name
}