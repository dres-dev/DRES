package dres.data.serializers

import dres.data.model.competition.Team
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import java.awt.Color
import java.nio.file.Paths

object TeamSerializer : Serializer<Team> {
    override fun serialize(out: DataOutput2, value: Team) {
        out.packLong(value.id)
        out.writeUTF(value.name)
        out.writeInt(value.number)
        out.writeInt(value.color.rgb)
        out.writeUTF(value.logo.toString())
    }

    override fun deserialize(input: DataInput2, available: Int): Team = Team(
            input.unpackLong(),
            input.readUTF(),
            input.readInt(),
            Color(input.readInt()),
            Paths.get(input.readUTF())
    )
}