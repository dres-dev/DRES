package dres.data.serializers

import dres.data.model.competition.Team
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.awt.Color
import java.nio.file.Paths

object TeamSerializer : Serializer<Team> {
    override fun serialize(out: DataOutput2, value: Team) {
        out.writeUTF(value.name)
        out.writeUTF(value.color)
        out.writeUTF(value.logo)
        out.packInt(value.users.size)
        value.users.forEach { out.packLong(it) }
    }

    override fun deserialize(input: DataInput2, available: Int): Team = Team(
        input.readUTF(),
        input.readUTF(),
        input.readUTF(),
        (0 until input.unpackInt()).map { input.unpackLong() }.toMutableList()
    )
}