package dev.dres.run.updatables

import dev.dres.data.model.template.options.SubmissionFilterOption
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManagerStatus
import java.util.concurrent.atomic.AtomicInteger

class EndTaskUpdatable(private val run: InteractiveRunManager, private val context: RunActionContext) : Updatable {

    override val phase: Phase = Phase.MAIN

    /** Number of submissions seen during the last update. */
    private var submissions = AtomicInteger(0)

    val isAsync = run is InteractiveAsynchronousRunManager

    override fun update(status: RunManagerStatus) {
        val taskRun = this.run.currentTask(this.context)
        if (taskRun != null) {
            val limitingFilter =
                taskRun.template.taskType.filter.find { it.option == SubmissionFilterOption.LIMIT_CORRECT_PER_TEAM }
                    ?: return
            val limit = limitingFilter.getAsInt("limit") ?: 1
            if (this.run.timeLeft(context) > 0) {
                if (this.submissions.getAndSet(taskRun.submissions.size) < taskRun.submissions.size) {

                    if (isAsync) {

                        if (this.run.submissions(this.context)
                                .count { it.teamId == context.teamId && it.status == SubmissionStatus.CORRECT } >= limit
                        ) {
                            this.run.abortTask(context)
                            this.submissions.set(0)
                        }

                    } else {

                        /* Determine of all teams have submitted . */
                        val allDone = this.run.template.teams.all { team ->
                            this.run.submissions(this.context)
                                .count { it.teamId == team.uid && it.status == SubmissionStatus.CORRECT } >= limit
                        }

                        /* Do all teams have reached the limit of correct submissions ? */
                        if (allDone) {
                            this.run.abortTask(context)
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