package dev.dres.data.model.run

import dev.dres.data.model.competition.TaskDescription
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.interfaces.SubmissionValidator

abstract class Task {

    /** Reference to the [TaskDescription] describing this [Task]. */
    abstract val taskDescription: TaskDescription

    /** The [SubmissionFilter] used to filter [Submission]s. */
    @Transient
    val filter: SubmissionFilter = taskDescription.newFilter()

    /** The [TaskRunScorer] used to update score for this [Task]. */
    @Transient
    val scorer: TaskRunScorer = taskDescription.newScorer()

    /** The [SubmissionValidator] used to validate [Submission]s. */
    @Transient
    val validator: SubmissionValidator = taskDescription.newValidator()

}