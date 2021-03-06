package dev.dres.run.score.interfaces

import dev.dres.data.model.submissions.Submission

/**
 * A [TaskScorer] implementation that can update scores incrementally on a [Submission] by [Submission] basis.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
interface IncrementalTaskScorer: TaskScorer {
    /**
     * Updates this [IncrementalTaskScorer]'s score just using a single submission.
     *
     * @param submission The [Submission] to update this [IncrementalTaskScorer] with.
     */
    fun update(submission: Submission)
}