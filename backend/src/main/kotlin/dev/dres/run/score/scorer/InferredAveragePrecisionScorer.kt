package dev.dres.run.score.scorer

import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import jetbrains.exodus.database.TransientEntityStore


/**
 *
 * Computes the Inferred Average Precision as used in TREC based on an ordered, partially assessed list of [AnswerSet]s.
 * See https://www-nlpir.nist.gov/projects/tv2006/infap/inferredAP.pdf for details.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
class InferredAveragePrecisionScorer( scoreable: Scoreable, store: TransientEntityStore?) : AbstractTaskScorer(scoreable, store) {

    companion object {

        private const val epsilon = 1e-6 //TODO check what TRECVID uses
        private fun infAP(elements: Sequence<AnswerSet>): Double {

            if (elements.none()) {
                return 0.0
            }

            var infAPSum = 0.0
            var judgements = 0
            var correct = 0
            var wrong = 0

            elements.forEachIndexed { index, answerSet ->

                val k = index + 1.0
                when (answerSet.status()) {
                    VerdictStatus.CORRECT -> {
                        ++judgements // |d100|
                        ++correct // |rel|

                        val ap = if (index == 0) { //special case for first document
                            1.0 //all are relevant so far, since there is only one so far, and it is relevant
                        } else {
                            (1.0 / k) + ((k - 1.0) / k) * ((judgements / (k - 1.0)) * ((correct + epsilon) / (correct + wrong + 2.0 * epsilon)))
                        }

                        infAPSum += ap

                    }

                    VerdictStatus.WRONG -> {
                        ++judgements
                        ++wrong // |nonrel|
                    }

                    else -> {}
                }

            }

            if (correct == 0) {
                return 0.0
            }

            return infAPSum / correct

        }

    }

    /**
     * Computes and returns the scores for this [InferredAveragePrecisionScorer] based on a [Sequence] of [Submission]s.
     *
     * The sole use of this method is to keep the implementing classes unit-testable (irrespective of the database).
     *
     * @param submissions A [Sequence] of [Submission]s to obtain scores for.
     * @return A [Map] of [TeamId] to calculated task score.
     */
    override fun calculateScores(submissions: Sequence<Submission>): Map<TeamId, Double> {
        return this.scoreable.teams.associateWith { teamId ->
            val answerSets = submissions.filter { it.teamId == teamId }.flatMap { it.answerSets() }.filter { it.taskId == this.scoreable.taskId }
            infAP(answerSets)
        }
    }
}