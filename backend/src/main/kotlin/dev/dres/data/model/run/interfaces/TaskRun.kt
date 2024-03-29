package dev.dres.data.model.run.interfaces

import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.IATaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.task.TaskTemplateId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.CachingTaskScorer

typealias TaskId = String

/**
 * Represents a [IATaskRun] solved by a DRES user or client.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface TaskRun: Run, Scoreable {
    /** The unique [TaskId] that identifies this [TaskRun]. */
    override val taskId: TaskId

    /** List of [TeamId]s that worked on this [TaskRun] */
    override val teams: List<TeamId>

    /** The unique [TaskTemplateId] that identifies the task template underpinning [TaskRun]. */
    val taskTemplateId: TaskTemplateId

    /** The current [ApiTaskStatus] of this [TaskRun]. This is typically a transient property. */
    val status: ApiTaskStatus

    /** Reference to the [EvaluationRun] this [IATaskRun] belongs to. */
    val evaluationRun: EvaluationRun

    /** The position of this [IATaskRun] within the enclosing [EvaluationRun]. */
    val position: Int

    /** Reference to the [ApiTaskTemplate] describing this [IATaskRun]. */
    val template: ApiTaskTemplate

    /** The [CachingTaskScorer] used to update score for this [IATaskRun]. */
    val scorer: CachingTaskScorer

    /**
     * Prepares this [TaskRun] for later starting.
     */
    fun prepare()


    /**
     * Returns a [List] of all [ApiSubmission]s that belong to this [TaskRun].
     *
     * @return [List] of [ApiSubmission]s
     */
    fun getSubmissions(): List<ApiSubmission>

}