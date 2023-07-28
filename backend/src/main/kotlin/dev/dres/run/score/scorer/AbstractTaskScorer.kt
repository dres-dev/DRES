package dev.dres.run.score.scorer

import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.mapDistinct

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractTaskScorer(override val scoreable: Scoreable): TaskScorer {

    /**
     *
     */
    override fun scoreMap(): Map<TeamId, Double> {
        val sequence = DbAnswerSet.filter { (it.task.id eq this@AbstractTaskScorer.scoreable.taskId) }.mapDistinct { it.submission }.asSequence()
        return this.calculateScores(sequence)
    }

    /**
     * Computes and returns the scores for this [KisTaskScorer] based on a [Sequence] of [Submission]s.
     *
     * The sole use of this method is to keep the implementing classes unit-testable (irrespective of the database).
     *
     * @param submissions A [Sequence] of [Submission]s to obtain scores for.
     * @return A [Map] of [TeamId] to calculated task score.
     */
    abstract fun calculateScores(submissions: Sequence<Submission>): Map<TeamId, Double>
}