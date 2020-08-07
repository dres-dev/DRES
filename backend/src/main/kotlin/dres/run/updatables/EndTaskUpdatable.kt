package dres.run.updatables

import dres.data.model.competition.TaskType
import dres.data.model.run.SubmissionStatus
import dres.run.RunManager
import dres.run.RunManagerStatus

class EndTaskUpdatable(private val run: RunManager) : Updatable {

    override val phase: Phase = Phase.MAIN

    override fun update(status: RunManagerStatus) {
        if(run.currentTaskRun?.task?.taskType?.filter?.contains(TaskType.SubmissionFilterType.ONE_CORRECT_PER_TEAM) == true){

            val correctSubmissionTeams = run.currentTaskRun!!.submissions.filter { it.status == SubmissionStatus.CORRECT }.map { it.team }.toSet()

            if (run.competitionDescription.teams.size == correctSubmissionTeams.size) { //all teams have a correct submission
                run.adjustDuration(1 - run.timeLeft().toInt())
            }

        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = status == RunManagerStatus.RUNNING_TASK
}