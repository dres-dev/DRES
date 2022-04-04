package dev.dres.data.serializers

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.InteractiveAsynchronousCompetition
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.NonInteractiveCompetition
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.submissions.Submission
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

class CompetitionRunSerializer(private val competitionSerializer: CompetitionSerializer): Serializer<Competition> {
    override fun serialize(out: DataOutput2, value: Competition) {
        when(value) {
            is InteractiveSynchronousCompetition -> out.packInt(1)
            is NonInteractiveCompetition -> out.packInt(2)
            is InteractiveAsynchronousCompetition -> out.packInt(3)
        }
        out.writeUID(value.id)
        out.writeUTF(value.name)
        this.competitionSerializer.serialize(out, value.description)
        RunPropertiesSerializer.serialize(out, value.properties)
        out.writeLong(value.started ?: -1)
        out.writeLong(value.ended ?: -1)
        out.writeInt(value.tasks.size)

        when(value){
            is InteractiveSynchronousCompetition -> {
                for (taskRun in value.tasks) {
                    out.writeUID(taskRun.uid)
                    out.writeUID(taskRun.taskDescriptionId)
                    out.writeLong(taskRun.started ?: -1)
                    out.writeLong(taskRun.ended ?: -1)
                    out.writeInt(taskRun.submissions.size)
                    for (submission in taskRun.submissions) {
                        SubmissionSerializer.serialize(out, submission)
                    }
                }
            }
            is NonInteractiveCompetition -> {
                //TODO
            }
            is InteractiveAsynchronousCompetition -> {
                for (taskRun in value.tasks) {
                    out.writeUID(taskRun.uid)
                    out.writeUID(taskRun.teamId)
                    out.writeUID(taskRun.descriptionId)
                    out.writeLong(taskRun.started ?: -1)
                    out.writeLong(taskRun.ended ?: -1)
                    out.writeInt(taskRun.submissions.size)
                    for (submission in taskRun.submissions) {
                        SubmissionSerializer.serialize(out, submission)
                    }
                }
                value.permutation.forEach { teamId, indices ->
                    out.writeUID(teamId)
                    indices.forEach { out.packInt(it) }
                }
            }
        }


    }

    override fun deserialize(input: DataInput2, available: Int): Competition {
        return when(val type = input.unpackInt()) {
            1 -> {
                val run = InteractiveSynchronousCompetition(input.readUTF().UID(), input.readUTF(), competitionSerializer.deserialize(input, available), RunPropertiesSerializer.deserialize(input, available), input.readLong(), input.readLong())
                for (i in 0 until input.readInt()) {
                    val taskRun = run.Task(input.readUID(), input.readUID(), input.readLong(), input.readLong())
                    for (j in 0 until input.readInt()) {
                        taskRun.submissions.add(SubmissionSerializer.deserialize(input,available))
                    }
                }
                run
            }
            2 -> {
                TODO()
            }
            3 -> {

                val id = input.readUID()
                val name = input.readUTF()
                val description = competitionSerializer.deserialize(input, available)
                val properties = RunPropertiesSerializer.deserialize(input, available)
                val start = input.readLong()
                val end = input.readLong()

                val tasks = mutableListOf<TaskContainer>()

                for (i in 0 until input.readInt()) {
                    val taskContainer = TaskContainer(input.readUID(), input.readUID(), input.readUID(), input.readLong(), input.readLong())
                    for (j in 0 until input.readInt()) {
                        taskContainer.submissions.add(SubmissionSerializer.deserialize(input,available))
                    }
                    tasks.add(taskContainer)
                }

                val permutations = (0 until description.teams.size).associate {
                    val teamId = input.readUID()
                    val indices = (0 until description.tasks.size).map {
                        input.unpackInt()
                    }
                    teamId to indices
                }

                val run = InteractiveAsynchronousCompetition(id, name, description, properties, start, end, permutations)

                tasks.forEach {
                    val taskRun = run.Task(it.runId, it.teamId, it.descriptionId, it.start, it.end)
                    it.submissions.forEach { s ->
                        taskRun.submissions.add(s)
                    }
                }

                run.reconstructNavigationMap()
                run
            }
            else -> throw IllegalArgumentException("Unknown CompetitionRun type: $type")
        }

    }

    data class TaskContainer(
        val runId: UID,
        val teamId: TeamId,
        val descriptionId: TaskDescriptionId,
        val start: Long,
        val end: Long,
        val submissions: MutableList<Submission> = mutableListOf()
    )

}