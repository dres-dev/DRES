package dev.dres.data.model.run.interfaces

import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.InteractiveAsynchronousCompetition.Task
import dev.dres.run.TaskRunStatus
import dev.dres.run.score.interfaces.TaskScorer

/**
 * Represents a [Task] solved by a DRES user or client.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Task: Run {
    /** The unique [TaskId] that identifies this [Task]. Used by the persistence layer. */
    val uid: TaskId

    /** Reference to the [Competition] this [Task] belongs to. */
    val competition: Competition

    /** The position of this [Task] within the enclosing [Competition]. */
    val position: Int

    /** Reference to the [TaskDescription] describing this [Task]. */
    val description: TaskDescription

    /** The [TaskScorer] used to update score for this [Task]. */
    val scorer: TaskScorer

    /** The current status of this [Task]. */
    val status: TaskRunStatus


}