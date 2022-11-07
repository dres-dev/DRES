package dev.dres.data.model.run.interfaces

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.Task
import dev.dres.run.TaskStatus
import dev.dres.run.score.interfaces.TaskScorer
typealias TaskId = String

/**
 * Represents a [Task] solved by a DRES user or client.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface TaskRun: Run {
    /** The unique [TaskId] that identifies this [Task]. Used by the persistence layer. */
    val id: TaskId

    /** Reference to the [EvaluationRun] this [Task] belongs to. */
    val competition: EvaluationRun

    /** The position of this [Task] within the enclosing [EvaluationRun]. */
    val position: Int

    /** Reference to the [TaskTemplate] describing this [Task]. */
    val template: TaskTemplate

    /** The [TaskScorer] used to update score for this [Task]. */
    val scorer: TaskScorer

    /** The current status of this [Task]. */
    val status: TaskStatus
}