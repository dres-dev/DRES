package dres.run.score

import dres.data.model.competition.Team
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun

class KisScoreBoard(private val name: String, private val run: CompetitionRun, private val scorer: KisTaskScorer, private val taskFilter: (TaskDescription) -> Boolean) : Scoreboard {

    private val maxPointsPerTask = 100.0
    private val maxScoreNormalized = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 20.0

    private val scorePerTaskMap = mutableMapOf<TaskDescription, Map<Int, Double>>()

//    override fun taskScores(): List<Score> {
//
//        val currentTask: TaskDescription = run.currentTask?.task ?: return emptyList()
//        return scorePerTaskMap[currentTask]?.map { Score(run.competition.teams.indexOf(it.key), it.value) } ?: emptyList()
//
//    }

    private fun overallScoreMap(): Map<Int, Double> {
        val scoreSums = scorePerTaskMap.values
                .flatMap {it.entries} //all team to score pairs independent of task
                .groupBy { it.key } //individual scores per team
                .mapValues {
                    it.value.map { i -> i.value }.sum()
                }

        val maxScore = scoreSums.values.max() ?: return emptyMap()

        return scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
    }

    override fun scores(): List<Score> {
        return overallScoreMap().entries.map {
            Score(it.key, it.value)
        }
    }

    override fun score(teamId: Int) = overallScoreMap()[teamId] ?: 0.0

    //TODO introduce some caching
    override fun update() {

        val runs = run.runs.filter { it.started != null && taskFilter(it.task) }

        val scoresPerTask = runs.map { it.task to scorer.analyze(it) }

        scorePerTaskMap.clear()
        scorePerTaskMap.putAll(scoresPerTask)
    }

    override fun name() = name
}