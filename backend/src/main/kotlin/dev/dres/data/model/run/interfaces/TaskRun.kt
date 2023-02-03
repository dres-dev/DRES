package dev.dres.data.model.run.interfaces

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.IATaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.TaskStatus
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.TaskScorer
typealias TaskId = String

/**
 * Represents a [IATaskRun] solved by a DRES user or client.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface TaskRun: Run {
    /** The unique [TaskId] that identifies this [IATaskRun]. Used by the persistence layer. */
    val id: TaskId

    /** Reference to the [EvaluationRun] this [IATaskRun] belongs to. */
    val competition: EvaluationRun

    /** The position of this [IATaskRun] within the enclosing [EvaluationRun]. */
    val position: Int

    /** Reference to the [DbTaskTemplate] describing this [IATaskRun]. */
    val template: DbTaskTemplate

    /** The [TaskScorer] used to update score for this [IATaskRun]. */
    val scorer: CachingTaskScorer

    /** The current status of this [IATaskRun]. */
    val status: TaskStatus

    /**
     * Prepares this [TaskRun] for later starting.
     */
    fun prepare()

    /**
     * Returns a [List] of all [DbSubmission]s that belong to this [TaskRun].
     *
     * @return [List] of [DbSubmission]s
     */
    fun getSubmissions(): Sequence<DbSubmission>
}