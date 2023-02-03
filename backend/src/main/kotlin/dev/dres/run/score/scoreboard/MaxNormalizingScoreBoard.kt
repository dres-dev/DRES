package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.template.TemplateId
import dev.dres.run.score.interfaces.TaskScorer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * A [Scoreboard] that keeps track of the maximum score per [DbTaskTemplate] as identified by it [TemplateId].
 *
 * @author Luca Rossett
 * @version 1.1.0
 */
class MaxNormalizingScoreBoard(override val name: String, teams: List<DbTeam>, private val taskFilter: (DbTaskTemplate) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 1000.0) : Scoreboard {

    /** Tracks the score per [TemplateId] (references a [DbTaskTemplate]). */
    private val scorePerTaskMap = ConcurrentHashMap<TemplateId, Map<TemplateId, Double>>()

    private val teamIds = teams.map { it.teamId }

    private fun overallScoreMap(): Map<TemplateId, Double> {
        val scoreSums = scorePerTaskMap.values
                .flatMap {it.entries} //all team to score pairs independent of task
                .groupBy { it.key } //individual scores per team
                .mapValues {
                    it.value.sumOf { i -> i.value }
                }

        val maxScore = max(1.0, scoreSums.values.maxOrNull() ?: return emptyMap())
        return scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
    }

    override fun scores(): List<Score> {
        val scores = overallScoreMap()
        return this.teamIds.map { Score(it, scores[it] ?: 0.0) }
    }

    override fun score(teamId: TeamId) = overallScoreMap()[teamId] ?: 0.0


    override fun update(scorers: Map<TemplateId, TaskScorer>) {
        this.scorePerTaskMap.clear()
        this.scorePerTaskMap.putAll(scorers.map {
            it.key to it.value.scores().groupBy { it.first }.mapValues {
                it.value.maxByOrNull { it.third }?.third ?: 0.0
            }
        }.toMap()
        )
    }

    override fun update(runs: List<AbstractInteractiveTask>) = update(
        runs.filter { taskFilter(it.template) && (it.started != null) }.associate { it.id to it.scorer }
    )

    override fun overview() = ScoreOverview(name, taskGroupName, scores())
}