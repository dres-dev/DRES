package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.InteractiveSynchronousCompetitionRun.TaskRun

class InteractiveAsynchronousCompetitionRun(override var id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription): CompetitionRun(id, name, competitionDescription)  {


    override val tasks: List<Task>
        get() = TODO("Not yet implemented")



    inner class TeamTaskRun (override val uid: TaskId = UID(), val teamId: TeamId, val taskDescriptionId: TaskDescriptionId): Run, InteractiveTask() {

        internal constructor(uid: TaskId, teamId: TeamId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, teamId, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** List of [Submission]s* registered for this [TaskRun]. */
        val submissions: List<Submission> = mutableListOf()

        override fun addSubmission(submission: Submission) {
            TODO("Not yet implemented")
        }

        override val taskDescription: TaskDescription
            get() = this@InteractiveAsynchronousCompetitionRun.competitionDescription
                .tasks.find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")



    }


}