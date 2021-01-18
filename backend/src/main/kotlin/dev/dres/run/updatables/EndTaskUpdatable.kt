package dev.dres.run.updatables

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus

class EndTaskUpdatable(private val run: RunManager) : Updatable {

    override val phase: Phase = Phase.MAIN

    private var running = true
    private var lastKnownTaskId: UID = UID.EMPTY

    override fun update(status: RunManagerStatus) {

        val limitingFilter = run.currentTaskRun?.task?.taskType?.filter?.find{ it.option == TaskType.SubmissionFilterType.LIMIT_CORRECT_PER_TEAM }
            ?: /* no filter */
            return

        val limit = limitingFilter.parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1

        /* Do we know the current task? */
        if (lastKnownTaskId == run.currentTask?.id) {
            /* are we still running ? */
            if (!running) {
                /* no, so we do not update anymore */
                /* i.e. the task was just finished because of us */
                return
            }
        } else {
            /* a new task we do not know, so we want to run again */
            running = true
        }
        /* let's keep track of the current task, we do this always as we are not specifically called upon task change */
        lastKnownTaskId = run.currentTask?.id!!

        /* fetch the correct submissions teams */
        val correctSubmissionTeams = run.currentTaskRun!!.submissions.filter {
            it.status == SubmissionStatus.CORRECT
        }.groupBy { it.teamId }.values.map { it.size }

        /* Do all teams have reached the limit of correct submissions ? */
        if (run.timeLeft() > 5000 &&
                correctSubmissionTeams.all { it >= limit }) {
            run.adjustDuration(1 - (run.timeLeft().toInt()) / 1000) /* run.timeLeft() is ms */
            running = false /* do not update anymore, as the task is over now */
        }


    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = status == RunManagerStatus.RUNNING_TASK
}