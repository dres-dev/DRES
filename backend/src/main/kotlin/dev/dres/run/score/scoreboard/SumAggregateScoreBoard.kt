package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.TemplateId
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.run.score.scorer.TaskScorer
import dev.dres.utilities.extensions.convertWriteLock
import dev.dres.utilities.extensions.write
import java.util.concurrent.locks.StampedLock

/**
 * A [Scoreboard] that keeps tracks the total score per team and task group.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class SumAggregateScoreBoard(override val name: String, override val run: EvaluationRun, private val boards: List<Scoreboard>, private val taskGroupName: String? = null) : Scoreboard {

    /** Flag indicating, that this [SumAggregateScoreBoard] is dirty and needs re-calculation. */
    override val dirty: Boolean = false

    override fun scores(): List<Score> = this.boards.map { it.scores() }
        .flatten().groupBy { it.teamId }.values
        .map { Score(it.first().teamId, it.sumOf { it.score }) }

    /**
     * Retrieves and returns the score of the given [TeamId]
     *
     * @param teamId The [TeamId] to retrieve the score for.
     * @return The score for the given [TeamId].
     */
    override fun score(teamId: TeamId) = boards.sumOf { it.score(teamId) }

    /**
     * Returns a summary of all current scores in this [MaxNormalizingScoreBoard]
     */
    override fun overview() = ScoreOverview(this.name, this.taskGroupName, scores())

    /**
     * Has no effect, since [SumAggregateScoreBoard] itself is stateless.
     */
    override fun invalidate() {
        /* No op. */
    }
}