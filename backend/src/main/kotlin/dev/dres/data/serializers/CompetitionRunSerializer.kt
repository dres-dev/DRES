package dev.dres.data.serializers

import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.InteractiveCompetitionRun
import dev.dres.data.model.run.NonInteractiveCompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

class CompetitionRunSerializer(private val competitionSerializer: CompetitionSerializer): Serializer<CompetitionRun> {
    override fun serialize(out: DataOutput2, value: CompetitionRun) {
        when(value) {
            is InteractiveCompetitionRun -> out.packInt(1)
            is NonInteractiveCompetitionRun -> out.packInt(2)
        }
        out.writeUID(value.id)
        out.writeUTF(value.name)
        competitionSerializer.serialize(out, value.competitionDescription)
        out.writeLong(value.started ?: -1)
        out.writeLong(value.ended ?: -1)
        out.writeInt(value.tasks.size)

        when(value){
            is InteractiveCompetitionRun -> {
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
            is NonInteractiveCompetitionRun -> {
                //TODO
            }
        }


    }

    override fun deserialize(input: DataInput2, available: Int): CompetitionRun {
        return when(val type = input.unpackInt()) {
            1 -> {
                val run = InteractiveCompetitionRun(input.readUTF().UID(), input.readUTF(), competitionSerializer.deserialize(input, available), input.readLong(), input.readLong())
                for (i in 0 until input.readInt()) {
                    val taskRun = run.TaskRun(input.readUID(), input.readUID(), input.readLong(), input.readLong())
                    for (j in 0 until input.readInt()) {
                        (taskRun.submissions as MutableList<Submission>).add(SubmissionSerializer.deserialize(input,available))
                    }
                    (run.tasks as MutableList<InteractiveCompetitionRun.TaskRun>).add(taskRun)
                }
                run
            }
            2 -> {
                TODO()
            }
            else -> throw IllegalArgumentException("Unknown CompetitionRun type: $type")
        }

    }
}