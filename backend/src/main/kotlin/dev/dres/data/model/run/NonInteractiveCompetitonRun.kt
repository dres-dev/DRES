package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId


open class NonInteractiveCompetitionRun(override var id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription): CompetitionRun(id, name, competitionDescription) {

    override val tasks: List<TaskContainer> = competitionDescription.tasks.map { TaskContainer(taskDescriptionId = it.id) }


    inner class TaskContainer(override val uid: TaskId = UID(), val taskDescriptionId: TaskDescriptionId) : NonInteractiveTask() {

        internal val submissions : MutableMap<Pair<TeamId, String>, ResultBatch<*>> = mutableMapOf()

        @Synchronized
        override fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>) {

            batches.forEach { resultBatch ->
                submissions[origin.teamId to resultBatch.name] = resultBatch
            }

        }

        override val taskDescription: TaskDescription = this@NonInteractiveCompetitionRun.competitionDescription.tasks
            .find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

    }




}