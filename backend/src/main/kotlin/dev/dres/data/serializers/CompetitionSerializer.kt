package dev.dres.data.serializers

import dev.dres.data.dbo.DAO
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

class CompetitionSerializer(private val mediaItems: DAO<MediaItem>): Serializer<CompetitionDescription> {
    override fun serialize(out: DataOutput2, value: CompetitionDescription) {
        val taskDescriptionSerializer = TaskDescriptionSerializer(value.taskGroups, value.taskTypes, this.mediaItems)
        out.writeUID(value.id)
        out.writeUTF(value.name)
        out.writeUTF(value.description ?: "")
        out.packInt(value.taskTypes.size)
        for (type in value.taskTypes) {
            TaskTypeSerializer.serialize(out, type)
        }
        out.packInt(value.taskGroups.size)
        for (task in value.taskGroups) {
            TaskGroupSerializer.serialize(out, task)
        }
        out.packInt(value.tasks.size)
        for (task in value.tasks) {
            taskDescriptionSerializer.serialize(out, task)
        }
        out.packInt(value.teams.size)
        for (team in value.teams) {
            TeamSerializer.serialize(out, team)
        }
        out.packInt(value.teamGroups.size)
        for (teamGroup in value.teamGroups) {
            TeamGroupSerializer.serialize(out, teamGroup)
        }
        out.packInt(value.judges.size)
        for (judge in value.judges) {
            out.writeUID(judge)
        }
        //out.writeBoolean(value.participantCanView)
        //out.writeBoolean(value.shuffleTasks)
    }

    override fun deserialize(input: DataInput2, available: Int): CompetitionDescription {
        val id = input.readUID()
        val name = input.readUTF()
        val description = input.readUTF()
        val taskTypes = (0 until input.unpackInt()).map { TaskTypeSerializer.deserialize(input, available) }.toMutableList()
        val taskGroups = (0 until input.unpackInt()).map { TaskGroupSerializer.deserialize(input, available) }.toMutableList()
        val taskDescriptionSerializer = TaskDescriptionSerializer(taskGroups, taskTypes, this.mediaItems)
        val tasks = (0 until input.unpackInt()).map { taskDescriptionSerializer.deserialize(input,available) }.toMutableList()
        val teams = (0 until input.unpackInt()).map { TeamSerializer.deserialize(input, available) }.toMutableList()
        val teamGroups = (0 until input.unpackInt()).map { TeamGroupSerializer.deserialize(input, teams) }.toMutableList()
        val judges = (0 until input.unpackInt()).map { input.readUID() }.toMutableList()
        return CompetitionDescription(id, name, description, taskTypes, taskGroups, tasks, teams, teamGroups, judges)
    }
}