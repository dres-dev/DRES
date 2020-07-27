package dres.run.score.scoreboard

import dres.data.model.competition.Team
import dres.data.model.competition.TaskDescription
import dres.data.model.run.CompetitionRun
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class MaxNormalizingScoreBoard(private val name: String, teams: List<Team>, private val taskFilter: (TaskDescription) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 100.0) : Scoreboard {

    private val scorePerTaskMap = ConcurrentHashMap<TaskDescription, Map<Int, Double>>()

    private val teamIds = teams.indices.toList().sorted()

    private fun overallScoreMap(): Map<Int, Double> {
        val scoreSums = scorePerTaskMap.values
                .flatMap {it.entries} //all team to score pairs independent of task
                .groupBy { it.key } //individual scores per team
                .mapValues {
                    it.value.map { i -> i.value }.sum()
                }

        val maxScore = max(1.0, scoreSums.values.max() ?: return emptyMap())

        return scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
    }

    override fun scores(): List<Score> {

        val scores = overallScoreMap()
        return teamIds.map { Score(it, scores[it] ?: 0.0) }

    }

    override fun score(teamId: Int) = overallScoreMap()[teamId] ?: 0.0


    override fun update(runs: List<CompetitionRun.TaskRun>) {

        val filteredRuns = runs.filter { it.started != null && taskFilter(it.task) }

        val scoresPerTask = filteredRuns.map { it.task to it.scorer.scores() }

        scorePerTaskMap.clear()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
        scorePerTaskMap.putAll(scoresPerTask)
    }

    override fun name() = name

    override fun overview() = ScoreOverview(name, taskGroupName, scores())
}