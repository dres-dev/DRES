package dev.dres.data.model.run

import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId


open class NonInteractiveCompetitionRun(override var id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription): CompetitionRun(id, name, competitionDescription) {

    override val tasks: List<TaskContainer> = TODO()



    inner class TaskContainer(val taskDescriptionId: TaskDescriptionId) : Task() {
        override val taskDescription: TaskDescription = this@NonInteractiveCompetitionRun.competitionDescription.tasks.find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

    }




}