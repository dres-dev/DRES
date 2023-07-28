package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.TemplateId
import dev.dres.utilities.extensions.convertWriteLock
import dev.dres.utilities.extensions.write
import java.util.concurrent.locks.StampedLock
import kotlin.math.max

/**
 * A [Scoreboard] that keeps track of the maximum score per [DbTaskTemplate] as identified by it [TemplateId].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MaxNormalizingScoreBoard(override val name: String, override val run: EvaluationRun, val teamIds: List<TeamId>, private val taskFilter: (DbTaskTemplate) -> Boolean, private val taskGroupName: String? = null, private val maxScoreNormalized: Double = 1000.0) : Scoreboard {

    /** A [StampedLock] to synchronise access to this [MaxNormalizingScoreBoard]. */
    private val lock = StampedLock()

    /** Tracks the score per [TemplateId] (references a [DbTaskTemplate]). */
    @Volatile
    private var scores: Map<TemplateId,Double> = emptyMap()

    /** Flag indicating, that this [MaxNormalizingScoreBoard] is dirty and needs re-calculation. */
    @Volatile
    override var dirty: Boolean = true
        private set

    /**
     * Returns all overall [Score]s tracked by this [MaxNormalizingScoreBoard].
     *
     * @return List of [Score] for this [MaxNormalizingScoreBoard].
     */
    override fun scores(): List<Score> {
        var stamp = this.lock.readLock()
        try {
            if (this.dirty) {
                stamp = this.lock.convertWriteLock(stamp)
                this.recalculate()
            }
            return this.teamIds.map { Score(it, this.scores[it] ?: 0.0) }
        } finally {
            this.lock.unlock(stamp)
        }
    }

    /**
     * Retrieves and returns the score of the given [TeamId]
     *
     * @param teamId The [TeamId] to retrieve the score for.
     * @return The score for the given [TeamId].
     */
    override fun score(teamId: TeamId): Double {
        var stamp = this.lock.readLock()
        try {
            if (this.dirty) {
                stamp = this.lock.convertWriteLock(stamp)
                recalculate()
            }
            return this.scores[teamId] ?: 0.0
        } finally {
            this.lock.unlock(stamp)
        }
    }

    /**
     * Returns a summary of all current scores in this [MaxNormalizingScoreBoard]
     */
    override fun overview(): ScoreOverview = ScoreOverview(this.name, this.taskGroupName, this.scores())

    /**
     * Invalidates the content held by this [MaxNormalizingScoreBoard].
     */
    override fun invalidate() = this.lock.write {
        this.dirty = true
    }

    /**
     * Internal function; calculates the overall score per team.
     */
    private fun recalculate() {
        val scorers = this.run.tasks.filter { this.taskFilter(it.template) && (it.started != null) }.map { it.scorer }
        val scores = scorers.associate { scorer ->
            scorer.scoreable.taskId to scorer.scores().groupBy { s -> s.first }.mapValues {
                it.value.maxByOrNull { s -> s.third }?.third ?: 0.0
            }
        }

        val scoreSums = scores.values
            .flatMap {it.entries} //all team to score pairs independent of task
            .groupBy { it.key } //individual scores per team
            .mapValues {
                it.value.sumOf { i -> i.value }
            }

        val maxScore = max(1.0, scoreSums.values.maxOrNull() ?: return)

        /* Update local score map. */
        this.scores = scoreSums.mapValues { it.value * maxScoreNormalized / maxScore }
        this.dirty = false
    }
}