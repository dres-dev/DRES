package dres.data.serializers

import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.data.model.run.TaskRunData
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
//            out.writeInt(taskRun.data.submissions.size)
//            for (submission in taskRun.data.submissions) {
//                SubmissionSerializer.serialize(out, submission)
//            }
            TaskRunDataSerializer.serialize(out, taskRun.data)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): CompetitionRun {
        val run = CompetitionRun(input.unpackLong(), input.readUTF(), CompetitionSerializer.deserialize(input, available), input.readUTF(), input.readLong(), input.readLong())
        for (i in 0 until input.readInt()) {
            val taskRun = run.TaskRun(input.readInt(), input.readUTF(), input.readLong(), input.readLong())
            taskRun.data.merge(TaskRunDataSerializer.deserialize(input, available, taskRun.task, taskRun.taskId))
//            for (j in 0 until input.readInt()) {
//                (taskRun.data.submissions as MutableList<Submission>).add(SubmissionSerializer.deserialize(input,available))
//            }

            (run.runs as MutableList<CompetitionRun.TaskRun>).add(taskRun)
        }
        return run
    }
}

object TaskRunDataSerializer {

    fun serialize(out: DataOutput2, value: TaskRunData) {
        out.packInt(value.submissions.size)
        value.submissions.forEach { SubmissionSerializer.serialize(out, it) }

        out.packInt(value.userSessions.size)
        value.userSessions.forEach {
            out.packLong(it.key)
            out.writeUTF(it.value)
        }

        out.packInt(value.sessionQueryEventLogs.size)
        value.sessionQueryEventLogs.forEach {
            out.writeUTF(it.key)
            out.packInt(it.value.size)
            it.value.forEach { QueryEventLogSerializer.serialize(out, it) }
        }

        out.packInt(value.sessionQueryResultLogs.size)
        value.sessionQueryResultLogs.forEach {
            out.writeUTF(it.key)
            out.packInt(it.value.size)
            it.value.forEach { QueryResultLogSerializer.serialize(out, it) }
        }

    }

    fun deserialize(input: DataInput2, available: Int, task: TaskDescription, taskId: Int): TaskRunData {

        val submissions = (0 until input.unpackInt()).map {SubmissionSerializer.deserialize(input, available)}

        val userSessions = (0 until input.unpackInt()).map{input.unpackLong() to input.readUTF()}.toMap()

        val sessionQueryEventLogs = (0 until input.unpackInt()).map{
            input.readUTF() to
                    (0 until input.unpackInt()).map{ QueryEventLogSerializer.deserialize(input, available) }.toMutableList()
        }.toMap()

        val sessionQueryResultLogs = (0 until input.unpackInt()).map{
            input.readUTF() to
                    (0 until input.unpackInt()).map{ QueryResultLogSerializer.deserialize(input, available) }.toMutableList()
        }.toMap()

        return TaskRunData(task, taskId, submissions, userSessions, sessionQueryResultLogs, sessionQueryEventLogs)
    }


}