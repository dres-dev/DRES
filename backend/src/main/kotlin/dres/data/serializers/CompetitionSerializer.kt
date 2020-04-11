package dres.data.serializers

import dres.data.model.competition.CompetitionDescription
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object CompetitionSerializer: Serializer<CompetitionDescription> {
    override fun serialize(out: DataOutput2, value: CompetitionDescription) {
        out.packLong(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
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
            input.unpackLong(),
            input.readUTF(),
            input.readUTF(),
            (0 until input.readInt()).map { TaskGroupSerializer.deserialize(input, available) }.toMutableList(),
            (0 until input.readInt()).map { TaskDescriptionSerializer.deserialize(input, available) }.toMutableList(),
            (0 until input.readInt()).map { TeamSerializer.deserialize(input, available) }.toMutableList()
    )
}