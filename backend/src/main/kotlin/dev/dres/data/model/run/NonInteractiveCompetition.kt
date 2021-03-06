package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.run.interfaces.CompetitionId
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.aspects.OriginAspect
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.score.interfaces.TaskScorer


class NonInteractiveCompetition(override var id: CompetitionId, override val name: String, override val description: CompetitionDescription): AbstractRun(), Competition {

    /** */
    override val tasks: List<TaskContainer> = this.description.tasks.map { TaskContainer(taskDescriptionId = it.id) }


    inner class TaskContainer(override val uid: TaskId = UID(), val taskDescriptionId: TaskDescriptionId) : AbstractNonInteractiveTask() {

        internal val submissions : MutableMap<Pair<TeamId, String>, ResultBatch<*>> = mutableMapOf()

        @Synchronized
        override fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>) {

            batches.forEach { resultBatch ->
                submissions[origin.teamId to resultBatch.name] = resultBatch
            }
        }


        override val competition: Competition
            get() = this@NonInteractiveCompetition

        override val position: Int
            get() = this@NonInteractiveCompetition.tasks.indexOf(this)

        override val description: TaskDescription = this@NonInteractiveCompetition.description.tasks
            .find { it.id == this.taskDescriptionId } ?: throw IllegalArgumentException("There is no task with ID ${this.taskDescriptionId}.")

        @Transient
        override val scorer: TaskScorer = description.newScorer()
    }
}