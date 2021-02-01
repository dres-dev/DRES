package dev.dres.run.updatables

import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import java.util.concurrent.atomic.AtomicInteger

class EndTaskUpdatable(private val run: RunManager) : Updatable {

    override val phase: Phase = Phase.MAIN

    /** Number of submissions seen during the last update. */
    private var submissions = AtomicInteger(0)

    override fun update(status: RunManagerStatus) {
        val limitingFilter = run.currentTaskRun?.task?.taskType?.filter?.find{ it.option == TaskType.SubmissionFilterType.LIMIT_CORRECT_PER_TEAM } ?: return
        val limit = limitingFilter.parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1
        if (this.run.timeLeft() > 0) {
            val taskRun = this.run.currentTaskRun
            if (taskRun != null && this.submissions.getAndSet(taskRun.submissions.size) < taskRun.submissions.size) {
                /* Determine of all teams have submitted . */
                val allDone = this.run.competitionDescription.teams.all { team ->
                    run.submissions.count { it.teamId == team.uid && it.status == SubmissionStatus.CORRECT  } >= limit
                }

                /* Do all teams have reached the limit of correct submissions ? */
                if (allDone) {
                    this.run.abortTask()
                    this.submissions.set(0)
                }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = (status == RunManagerStatus.RUNNING_TASK)
}