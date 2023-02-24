package dev.dres.run.score.scorer

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.TaskScorer


/**
 * @author Luca Rossetto
 * @version 1.0.0
 */
class InferredAveragePrecisionScorer(override val scoreable: Scoreable) : TaskScorer {


    private val epsilon = 0.01 //TODO check what TRECVID uses
    override fun scoreMap(submissions: Sequence<Submission>): Map<TeamId, Double> {
        TODO("Not yet implemented")
    }

    // TODO: Fix and make purpose and functionality of class explicit through proper documentation.
    //see https://www-nlpir.nist.gov/projects/tv2006/infap/inferredAP.pdf
    /*fun infAP(elements: List<StatusAspect>): Double {

        if (elements.isEmpty()) {
            return 0.0
        }

        var infAPSum = 0.0
        var judgements = 0
        var correct = 0
        var wrong = 0

        elements.forEachIndexed { index, statusAspect ->

            val k = index + 1.0
            when(statusAspect.status) {
                VerdictStatus.CORRECT -> {
                    ++judgements // |d100|
                    ++correct // |rel|

                    val ap = if (index == 0){ //special case for first document
                        1.0 //all are relevant so far, since there is only one so far and it is relevant
                    } else {
                        (1.0 / k) + ((k - 1.0) / k) * ((judgements / (k - 1.0)) * ((correct + epsilon) / (correct + wrong + 2.0 * epsilon)))
                    }

                    println(ap)

                    infAPSum += ap

                }
                VerdictStatus.WRONG -> {
                    ++judgements
                    ++wrong // |nonrel|
                }
                else -> {}
            }

        }

        if (correct == 0){
            return 0.0
        }

        return infAPSum / correct

    }

    fun score(submissions: List<Submission>): Double = infAP(submissions)
    fun score(batch: ResultBatch<*>): Double = infAP(batch.results)
    */


    /*override fun computeScores(batch: ResultBatch<*>): Double = this.lastScoresLock.write {
        val score = score(batch)
        this.lastScores[batch.teamId to batch.name] = score
        return@write score
    }*/

//    private var lastScores: MutableMap<Pair<TeamId, String>, Double> = mutableMapOf()
//
//    private val lastScoresLock = ReentrantReadWriteLock()
//
//
//    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
//        this.lastScores.map { ScoreEntry(it.key.first, it.key.second, it.value) }
//    }
}