package dres.data.serializers

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.CompetitionDescription
import dres.utilities.extensions.readUID
import dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

class CompetitionSerializer(private val mediaItems: DAO<MediaItem>): Serializer<CompetitionDescription> {
    override fun serialize(out: DataOutput2, value: CompetitionDescription) {
        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
        out.writeInt(value.taskTypes.size)
        for (type in value.taskTypes) {
            TaskTypeSerializer.serialize(out, type)
        }
        out.writeInt(value.taskGroups.size)
        for (task in value.taskGroups) {
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

    override fun deserialize(input: DataInput2, available: Int): CompetitionDescription {

        val id = input.readUID()
        val name = input.readUTF()
        val description = input.readUTF()
        val taskTypes = (0 until input.readInt()).map { TaskTypeSerializer.deserialize(input, available) }.toMutableList()
        val groups = (0 until input.readInt()).map { TaskGroupSerializer.deserialize(input, available) }.toMutableList()
        val tasks = (0 until input.readInt()).map { TaskDescriptionSerializer.deserialize(input, groups, taskTypes, mediaItems) }.toMutableList()
        val teams = (0 until input.readInt()).map { TeamSerializer.deserialize(input, available) }.toMutableList()

        return CompetitionDescription(
                id, name, description, taskTypes, groups, tasks, teams
        )
    }
}