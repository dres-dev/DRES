package dev.dres.data.serializers

import dev.dres.data.model.competition.Team
import dev.dres.data.model.competition.TeamGroup
import dev.dres.data.model.competition.TeamGroupAggregation
import dev.dres.utilities.extensions.readUID
import dev.dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TeamGroupSerializer {
    fun serialize(out: DataOutput2, value: TeamGroup) {
        out.writeUID(value.uid)
        out.writeUTF(value.name)
        out.packInt(value.teams.size)
        value.teams.forEach { out.writeUID(it.uid) }
        out.writeUTF(value.aggregation.name)
    }

    fun deserialize(input: DataInput2, allTeams: List<Team>): TeamGroup {
        val uid = input.readUID()
        val name = input.readUTF()
        val teams = (0 until input.unpackInt()).mapNotNull {
            val teamId = input.readUID()
            allTeams.find { it.uid == teamId }
        }
        val aggregation = TeamGroupAggregation.valueOf(input.readUTF())
        return TeamGroup(uid, name, teams, aggregation)
    }
}