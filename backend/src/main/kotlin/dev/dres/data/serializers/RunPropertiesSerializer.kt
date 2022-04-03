package dev.dres.data.serializers

import dev.dres.data.model.run.RunProperties
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object RunPropertiesSerializer : Serializer<RunProperties> {
    override fun serialize(out: DataOutput2, value: RunProperties) {
        out.writeBoolean(value.participantCanView)
        out.writeBoolean(value.shuffleTasks)
        out.writeBoolean(value.allowRepeatedTasks)
    }

    override fun deserialize(input: DataInput2, available: Int): RunProperties = RunProperties(
        input.readBoolean(),
        input.readBoolean(),
        input.readBoolean()
    )
}