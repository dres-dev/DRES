package dev.dres.data.serializers

import dev.dres.data.model.run.InteractiveAsynchronousCompetition
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.NonInteractiveCompetition
import dev.dres.data.model.run.interfaces.Competition
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
            }
        }


    }

    override fun deserialize(input: DataInput2, available: Int): Competition {
        return when(val type = input.unpackInt()) {
            1 -> {
                val run = InteractiveSynchronousCompetition(input.readUTF().UID(), input.readUTF(), competitionSerializer.deserialize(input, available), input.readLong(), input.readLong())
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
                val run = InteractiveAsynchronousCompetition(input.readUTF().UID(), input.readUTF(), competitionSerializer.deserialize(input, available), input.readLong(), input.readLong())

                for (i in 0 until input.readInt()) {
                    val taskRun = run.Task(input.readUID(), input.readUID(), input.readUID(), input.readLong(), input.readLong())
                    for (j in 0 until input.readInt()) {
                        taskRun.submissions.add(SubmissionSerializer.deserialize(input,available))
                    }
                }
                run
            }
            else -> throw IllegalArgumentException("Unknown CompetitionRun type: $type")
        }

    }
}