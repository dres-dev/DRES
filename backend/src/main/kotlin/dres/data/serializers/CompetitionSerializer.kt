package dres.data.serializers

import dres.data.model.competition.CompetitionDescription
import dres.utilities.extensions.UID
import dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object CompetitionSerializer: Serializer<CompetitionDescription> {
    override fun serialize(out: DataOutput2, value: CompetitionDescription) {
        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
        out.writeInt(value.taskTypes.size)
        for (type in value.taskTypes) {
            TaskTypeSerializer.serialize(out, type)
        }
        out.writeInt(value.groups.size)
        for (task in value.groups) {
            TaskGroupSerializer.serialize(out, task)
        }
        out.writeInt(value.tasks.size)
        for (task in value.tasks) {
            TaskDescriptionSerializer.serialize(out, task)
        }
        out.writeInt(value.teams.size)
        for (team in value.teams) {
            TeamSerializer.serialize(out, team)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): CompetitionDescription = CompetitionDescription(
            input.readUTF().UID(),
            input.readUTF(),
            input.readUTF(),
            (0 until input.readInt()).map { TaskTypeSerializer.deserialize(input, available) }.toMutableList(),
            (0 until input.readInt()).map { TaskGroupSerializer.deserialize(input, available) }.toMutableList(),
            (0 until input.readInt()).map { TaskDescriptionSerializer.deserialize(input, available) }.toMutableList(),
            (0 until input.readInt()).map { TeamSerializer.deserialize(input, available) }.toMutableList()
    )
}