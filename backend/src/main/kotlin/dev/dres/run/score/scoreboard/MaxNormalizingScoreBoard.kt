package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.TemplateId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * A [Scoreboard] that keeps track of the maximum score per [DbTaskTemplate] as identified by it [TemplateId].
 *
 * @author Luca Rossett
 * @version 1.1.0
 */
class MaxNormalizingScoreBoard(override val name: String, override val run: EvaluationRun, val teamIds: List<TeamId>, private val taskFilter: (DbTaskTemplate) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 1000.0) : Scoreboard {

    /** Tracks the score per [TemplateId] (references a [DbTaskTemplate]). */
    private val scorePerTaskMap = ConcurrentHashMap<TemplateId, Map<TemplateId, Double>>()

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

    override fun update() {
        this.scorePerTaskMap.clear()
        val scorers = this.run.tasks.filter { this.taskFilter(it.template) && (it.started != null) }.map { it.scorer }
        this.scorePerTaskMap.putAll(scorers.associate { scorer ->
            scorer.scoreable.taskId to scorer.scoreListFromCache().groupBy { s -> s.first }.mapValues {
                it.value.maxByOrNull { s -> s.third }?.third ?: 0.0
            }
        })
    }

    override fun overview() = ScoreOverview(name, taskGroupName, scores())
}