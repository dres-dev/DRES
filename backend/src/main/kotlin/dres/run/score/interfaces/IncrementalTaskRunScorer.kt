package dres.run.score.interfaces

import dres.data.model.run.Submission

/**
 * A [TaskRunScorer] that can update scores incrementally on a [Submission] by [Submission] basis.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface IncrementalTaskRunScorer {
    /**
     * Updates this [TaskRunScorer]'s score just using a single submission. Not all types of
     * [TaskRunScorer]'s support incremental updating. If not, this method should throw an
     * [UnsupportedOperationException].
     *
     * @param submission The [Submission] to update this [TaskRunScorer] with.
     */
    fun update(submission: Submission)

    /**
     * Returns the current scores for all teams in the relevant Task
     */
    fun scores(): Map<Int, Double>
}