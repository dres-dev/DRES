package dres.run.updatables

import dres.data.model.competition.TaskType
import dres.data.model.run.SubmissionStatus
import dres.run.RunManager
import dres.run.RunManagerStatus

class EndTaskUpdatable(private val run: RunManager) : Updatable {

    override val phase: Phase = Phase.MAIN

    private var running = true

    override fun update(status: RunManagerStatus) {
        if(run.currentTaskRun?.task?.taskType?.filter?.contains(TaskType.SubmissionFilterType.ONE_CORRECT_PER_TEAM) == true){

            val correctSubmissionTeams = run.currentTaskRun!!.submissions.filter { it.status == SubmissionStatus.CORRECT }.map { it.team }.toSet()

            if (run.competitionDescription.teams.size == correctSubmissionTeams.size) { //all teams have a correct submission
                run.adjustDuration(1 - (run.timeLeft().toInt())/1000) /* run.timeLeft() is ms */
                running = false /* do not update anymore, as the task is over now */
            }

        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = running && status == RunManagerStatus.RUNNING_TASK
}