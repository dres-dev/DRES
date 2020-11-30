package dev.dres.run.score.scoreboard

import dev.dres.data.model.UID
import dev.dres.data.model.competition.Team
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.CompetitionRun
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class MaxNormalizingScoreBoard(override val name: String, teams: List<Team>, private val taskFilter: (TaskDescription) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 100.0) : Scoreboard {

    private val scorePerTaskMap = ConcurrentHashMap<TaskDescription, Map<UID, Double>>()

    private val teamIds = teams.map { it.uid }

    private fun overallScoreMap(): Map<UID, Double> {
        val scoreSums = scorePerTaskMap.values
                .flatMap {it.entries} //all team to score pairs independent of task
                .groupBy { it.key } //individual scores per team
                .mapValues {
                    it.value.map { i -> i.value }.sum()
                }

        val maxScore = max(1.0, scoreSums.values.maxOrNull() ?: return emptyMap())
        return scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
    }

    override fun scores(): List<Score> {
        val scores = overallScoreMap()
        return this.teamIds.map { Score(it.string, scores[it] ?: 0.0) }
    }

    override fun score(teamId: UID) = overallScoreMap()[teamId] ?: 0.0

    override fun update(runs: List<CompetitionRun.TaskRun>) {
        val filteredRuns = runs.filter { it.started != null && taskFilter(it.task) }
        val scoresPerTask = filteredRuns.map { it.task to it.scorer.scores() }
        this.scorePerTaskMap.clear()
        this.scorePerTaskMap.putAll(scoresPerTask)
    }

    override fun overview() = ScoreOverview(name, taskGroupName, scores())
}