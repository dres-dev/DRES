package dev.dres.run.updatables

import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.template.task.options.SubmissionOption
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import kotlinx.dnq.query.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * An [Updatable] that takes care of ending a task if enough submissions have been received.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class EndTaskUpdatable(private val run: InteractiveRunManager, private val context: RunActionContext) : Updatable {

    /** The [EndTaskUpdatable] always belongs to the [Phase.MAIN]. */
    override val phase: Phase = Phase.MAIN

    /** Number of submissions seen during the last update. */
    private var submissions = AtomicInteger(0)

    /** Internal flag indicating whether the provided [InteractiveRunManager] is asynchronous. */
    private val isAsync = this.run is InteractiveAsynchronousRunManager

    override fun update(status: RunManagerStatus) {
        val taskRun = this.run.currentTask(this.context)
        if (taskRun != null) {
            if (taskRun.template.taskGroup.type.submission.contains(SubmissionOption.LIMIT_CORRECT_PER_TEAM)) {
                val limit = taskRun.template.taskGroup.type.configurations.filter { it.key eq SubmissionOption.LIMIT_CORRECT_PER_TEAM.description }.firstOrNull()?.key?.toIntOrNull() ?: 1
                if (this.run.timeLeft(this.context) > 0) {
                    if (this.submissions.getAndSet(taskRun.submissions.size) < taskRun.submissions.size) {
                        val allDone = if (this.isAsync) {
                            val numberOfSubmissions = this.run.submissions(this.context).count { it.team.id == context.teamId && it.verdicts.first().status == VerdictStatus.CORRECT }
                            numberOfSubmissions >= limit
                        } else {
                            /* Determine of all teams have submitted . */
                            this.run.template.teams.asSequence().all { team ->
                                val numberOfSubmissions = this.run.submissions(this.context).count { it.team.id == team.teamId && it.verdicts.first().status == VerdictStatus.CORRECT }
                                numberOfSubmissions >= limit
                            }
                        }
                        if (allDone) {
                            this.run.abortTask(this.context)
                            this.submissions.set(0)
                        }
                    }
                }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean =
        (status == RunManagerStatus.ACTIVE) //FIXME needs to also check status of the task run
}