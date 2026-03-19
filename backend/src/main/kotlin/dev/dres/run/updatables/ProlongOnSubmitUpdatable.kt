package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.task.options.Defaults
import dev.dres.data.model.template.task.options.Parameters
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import kotlinx.dnq.query.*

/**
 * An [Updatable] that takes care of prolonging a task if a last-minute [DbSubmission] was received.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ProlongOnSubmitUpdatable(private val manager: InteractiveRunManager): Updatable {

    /** The [ProlongOnSubmitUpdatable] belongs to the [Phase.MAIN]. */
    override val phase: Phase = Phase.MAIN

    /**
     * Determines if the current task should be prolonged based on the last [DbSubmission] and does so if necessary.
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null!
     * @param context The [RunActionContext] used to invoke this [Updatable].
     */
    override fun update(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?, context: RunActionContext) {
        if (runStatus == RunManagerStatus.ACTIVE && taskStatus == ApiTaskStatus.RUNNING) {
            val currentTask = this.manager.currentTask(context) ?: return
            /* This is only sensible to do, if the task has a duration */
            if(currentTask.duration == null){
                return
            }
            val taskType = this.manager.template.taskTypes.firstOrNull { it.name == currentTask.template.taskType }!!
            val prolongOnSubmit = taskType.taskOptions.contains(ApiTaskOption.PROLONG_ON_SUBMISSION)
            if (prolongOnSubmit) {
                /* Retrieve relevant parameters. */
                val limit = taskType.configuration[Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM]?.toIntOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_LIMIT_DEFAULT
                val prolongBy = taskType.configuration[Parameters.PROLONG_ON_SUBMISSION_BY_PARAM]?.toIntOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_BY_DEFAULT
                val correctOnly = taskType.configuration[Parameters.PROLONG_ON_SUBMISSION_CORRECT_PARAM]?.toBooleanStrictOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_CORRECT_DEFAULT

                /* Apply prolongation if necessary. */
                val lastSubmission: DbSubmission? = DbAnswerSet.filter { it.task.id eq currentTask.taskId }.mapDistinct { it.submission }.lastOrNull()
                if (lastSubmission == null || (correctOnly && lastSubmission.answerSets.asSequence().all { it.status != DbVerdictStatus.CORRECT })) {
                    return
                }
                val timeLeft = Math.floorDiv(this.manager.timeLeft(context)!!, 1000)
                if (timeLeft in 0 until limit) {
                    this.manager.adjustDuration(context, prolongBy)
                }
            }
        }
    }

    /**
     * Returns true, if the [EndOnSubmitUpdatable] should be updated given the [RunManagerStatus] and the [ApiEvaluationState].
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null
     * @return True if update is required, which is while a task is running.
     */
    override fun shouldBeUpdated(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?): Boolean =
        (runStatus == RunManagerStatus.ACTIVE && taskStatus == ApiTaskStatus.RUNNING)
}
