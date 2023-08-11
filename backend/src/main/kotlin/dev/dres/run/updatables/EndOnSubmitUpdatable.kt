package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.template.tasks.options.ApiSubmissionOption
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.task.options.DbSubmissionOption
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import kotlinx.dnq.query.*

/**
 * An [Updatable] that takes care of ending a task if enough submissions have been received.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.2.0
 */
class EndOnSubmitUpdatable(private val manager: InteractiveRunManager, private val context: RunActionContext = RunActionContext.INTERNAL) : Updatable {

    /** The [EndOnSubmitUpdatable] always belongs to the [Phase.MAIN]. */
    override val phase: Phase = Phase.MAIN

    /**
     * Determines if the current task should be aborted based how many correct submissions have been received per team.
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null!
     * @param context The [RunActionContext] used to invoke this [Updatable].
     */
    override fun update(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?, context: RunActionContext) {
        if (runStatus == RunManagerStatus.ACTIVE && taskStatus == ApiTaskStatus.RUNNING)  {
            /* Get list of teams and list of submissions. */
            val currentTask = this.manager.currentTask(this.context) ?: return

            /* Determine of endOnSubmit is true for given task. */
            val taskType = this.manager.template.taskTypes.firstOrNull { it.name == currentTask.template.taskType }!!
            val endOnSubmit = taskType.submissionOptions.contains(ApiSubmissionOption.LIMIT_CORRECT_PER_TEAM)
            if (endOnSubmit) {
                val teams = currentTask.teams.associateWith { 0 }.toMutableMap()
                val submissions = currentTask.getDbSubmissions().toList()

                /* If there is no submission, we can abort here. */
                if (submissions.isNotEmpty()) {
                    val limit = taskType.configuration["LIMIT_CORRECT_PER_TEAM"]?.toIntOrNull() ?: 1

                    /* Count number of correct submissions per team. */
                    if (this.manager.timeLeft(this.context) > 0) {
                        for (s in submissions) {
                            for (a in s.answerSets.toList()) {
                                if (a.status == DbVerdictStatus.CORRECT) {
                                    teams[s.team.id] = teams[s.team.id]!! + 1
                                }
                            }
                        }

                        /* If all teams have reached the limit, end the task. */
                        if (teams.all { it.value >= limit }) {
                            this.manager.abortTask(this.context)
                        }
                    }
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