package dev.dres.run.score.interfaces

import dev.dres.data.model.submissions.DbSubmission

/**
 * A [TaskScorer] implementation that can update scores incrementally on a [DbSubmission] by [DbSubmission] basis.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
interface IncrementalSubmissionTaskScorer: TaskScorer {
    /**
     * Updates this [IncrementalSubmissionTaskScorer]'s score just using a single submission.
     *
     * @param submission The [DbSubmission] to update this [IncrementalSubmissionTaskScorer] with.
     */
    fun update(submission: DbSubmission)
}