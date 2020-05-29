package dres.data.serializers

import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object CompetitionRunSerializer: Serializer<CompetitionRun> {
    override fun serialize(out: DataOutput2, value: CompetitionRun) {
        out.packLong(value.id)
        out.writeUTF(value.name)
        CompetitionSerializer.serialize(out, value.competitionDescription)
        out.writeUTF(value.uid)
        out.writeLong(value.started ?: -1)
        out.writeLong(value.ended ?: -1)
        out.writeInt(value.runs.size)
        for (taskRun in value.runs) {
            out.writeInt(taskRun.taskId)
            out.writeUTF(taskRun.uid)
            out.writeLong(taskRun.started ?: -1)
            out.writeLong(taskRun.ended ?: -1)
            out.writeInt(taskRun.data.submissions.size)
            for (submission in taskRun.data.submissions) {
                SubmissionSerializer.serialize(out, submission)
            }
        }
    }

    override fun deserialize(input: DataInput2, available: Int): CompetitionRun {
        val run = CompetitionRun(input.unpackLong(), input.readUTF(), CompetitionSerializer.deserialize(input, available), input.readUTF(), input.readLong(), input.readLong())
        for (i in 0 until input.readInt()) {
            val taskRun = run.TaskRun(input.readInt(), input.readUTF(), input.readLong(), input.readLong())
            for (j in 0 until input.readInt()) {
                (taskRun.data.submissions as MutableList<Submission>).add(SubmissionSerializer.deserialize(input,available))
            }
            (run.runs as MutableList<CompetitionRun.TaskRun>).add(taskRun)
        }
        return run
    }
}