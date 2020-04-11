package dres.data.model.competition.interfaces

import dres.data.model.competition.TaskGroup
import dres.run.score.TaskRunScorer
import dres.run.validate.SubmissionValidator

/**
 * Basic description of a [Task].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface TaskDescription {
    /** The name of the task */
    val name: String

    /** The [TaskGroup]  the [Task] belongs to */
    val taskGroup: TaskGroup

    /** The duration of the [TaskDescription] in seconds. */
    val duration: Long

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription].
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription].
     *
     * @return [SubmissionValidator].
     */
    fun newValidator(): SubmissionValidator
}