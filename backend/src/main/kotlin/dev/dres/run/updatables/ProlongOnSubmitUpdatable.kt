package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.template.task.options.Defaults
import dev.dres.data.model.template.task.options.Parameters
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import kotlinx.dnq.query.any
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull

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
            val prolongOnSubmit = currentTask.template.taskGroup.type.options.filter { it.description eq DbTaskOption.PROLONG_ON_SUBMISSION.description }.any()
            if (prolongOnSubmit) {
                /* Retrieve relevant parameters. */
                val limit = currentTask.template.taskGroup.type.configurations.filter {
                    it.key eq Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM
                }.firstOrNull()?.value?.toIntOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_LIMIT_DEFAULT
                val prolongBy = currentTask.template.taskGroup.type.configurations.filter {
                    it.key eq Parameters.PROLONG_ON_SUBMISSION_BY_PARAM
                }.firstOrNull()?.value?.toIntOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_BY_DEFAULT
                val correctOnly = currentTask.template.taskGroup.type.configurations.filter {
                    it.key eq Parameters.PROLONG_ON_SUBMISSION_CORRECT_PARAM
                }.firstOrNull()?.value?.toBooleanStrictOrNull() ?: Defaults.PROLONG_ON_SUBMISSION_CORRECT_DEFAULT

                /* Apply prolongation if necessary. */
                val submission: DbSubmission? = this.manager.currentSubmissions(context).lastOrNull()
                if (submission == null || (correctOnly && submission.answerSets().all { it.status() != VerdictStatus.CORRECT })) {
                    return
                }
                val timeLeft = Math.floorDiv(this.manager.timeLeft(context), 1000)
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