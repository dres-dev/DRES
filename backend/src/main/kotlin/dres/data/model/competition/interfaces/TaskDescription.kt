package dres.data.model.competition.interfaces

import dres.data.model.competition.TaskGroup
import dres.data.model.run.Submission
import dres.run.filter.AllSubmissionFilter
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.validation.interfaces.SubmissionValidator

/**
 * Basic description of a [Task].
 *
 * @author Ralph Gasser
 * @version 1.1
 */
interface TaskDescription {

    /** Internal, unique ID of this [TaskDescription]. */
    val uid: String

    /** The name of the task */
    val name: String

    /** The [TaskGroup]  the [Task] belongs to */
    val taskGroup: TaskGroup

    /** The duration of the [TaskDescription] in seconds. */
    val duration: Long

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @param callback An optional callback function that should be registered with the [SubmissionValidator].
     * @return [SubmissionValidator].
     */
    fun newValidator(callback: ((Submission) -> Unit)? = null): SubmissionValidator

    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter = AllSubmissionFilter
}