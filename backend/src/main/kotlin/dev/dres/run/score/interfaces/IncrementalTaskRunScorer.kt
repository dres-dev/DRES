package dev.dres.run.score.interfaces

import dev.dres.data.model.submissions.Submission

/**
 * A [RecalculatingTaskRunScorer] that can update scores incrementally on a [Submission] by [Submission] basis.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface IncrementalTaskRunScorer: TaskRunScorer {
    /**
     * Updates this [RecalculatingTaskRunScorer]'s score just using a single submission. Not all types of
     * [RecalculatingTaskRunScorer]'s support incremental updating. If not, this method should throw an
     * [UnsupportedOperationException].
     *
     * @param submission The [Submission] to update this [RecalculatingTaskRunScorer] with.
     */
    fun update(submission: Submission)

}