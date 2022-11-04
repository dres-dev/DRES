package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.task.TaskDescriptionId
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.run.score.interfaces.TaskScorer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class MaxNormalizingScoreBoard(override val name: String, teams: List<Team>, private val taskFilter: (TaskTemplate) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 100.0) : Scoreboard {

    private val scorePerTaskMap = ConcurrentHashMap<TaskDescriptionId, Map<TaskDescriptionId, Double>>()

    private val teamIds = teams.map { it.teamId }

    private fun overallScoreMap(): Map<TaskDescriptionId, Double> {
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
        return this.teamIds.map { Score(it, scores[it] ?: 0.0) }
    }

    override fun score(teamId: TeamId) = overallScoreMap()[teamId] ?: 0.0


    override fun update(scorers: Map<TaskDescriptionId, TaskScorer>) {
        this.scorePerTaskMap.clear()
        this.scorePerTaskMap.putAll(scorers.map {
            it.key to it.value.scores().groupBy { it.first }.mapValues {
                it.value.maxByOrNull { it.third }?.third ?: 0.0
            }
        }.toMap()
        )
    }

    override fun update(runs: List<AbstractInteractiveTask>) {
        update(
            runs
            .filter { taskFilter(it.template) && (it.started != null) }
            .map { it.uid to it.scorer }.toMap()
        )
    }

    override fun overview() = ScoreOverview(name, taskGroupName, scores())
}