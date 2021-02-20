package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.run.validation.interfaces.SubmissionBatchValidator


open class NonInteractiveCompetitionRun(override var id: CompetitionRunId, name: String, competitionDescription: CompetitionDescription): CompetitionRun(id, name, competitionDescription) {

    override val tasks: List<TaskContainer> = competitionDescription.tasks.map { TaskContainer(taskDescriptionId = it.id) }


    inner class TaskContainer(override val uid: TaskId = UID(), val taskDescriptionId: TaskDescriptionId) : NonInteractiveTask() {

        private val submissions : MutableMap<TeamId, MutableMap<String, List<ItemAspect>>> = mutableMapOf()

        override fun addSubmissionBatch(origin: OriginAspect, batches: List<BaseBatch<*>>) {

            if (!submissions.containsKey(origin.teamId)) {
                submissions[origin.teamId] = mutableMapOf()
            }

            val map = submissions[origin.teamId]!!

            batches.forEach {
                map[it.name] = it.results
            }


        }

        override val validator: SubmissionBatchValidator
            get() = TODO("Not yet implemented")

        override val taskDescription: TaskDescription = this@NonInteractiveCompetitionRun.competitionDescription.tasks.find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

    }




}