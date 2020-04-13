package dres.run.score.scoreboard

import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun

class MaxNormalizingScoreBoard(private val name: String, private val run: CompetitionRun, private val taskFilter: (TaskDescription) -> Boolean, private val maxScoreNormalized: Double = 100.0) : Scoreboard {

    private val scorePerTaskMap = mutableMapOf<TaskDescription, Map<Int, Double>>()


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


    override fun update() {

        val runs = run.runs.filter { it.started != null && taskFilter(it.task) }

        val scoresPerTask = runs.map { it.task to it.scorer.scores() }

        scorePerTaskMap.clear()
        scorePerTaskMap.putAll(scoresPerTask)
    }

    override fun name() = name
}