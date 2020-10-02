package dev.dres.data.serializers

import dev.dres.data.model.basics.time.TemporalRange
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TemporalRangeSerializer: Serializer<TemporalRange> {
    override fun serialize(out: DataOutput2, value: TemporalRange) {
        TemporalPointSerializer.serialize(out, value.start)
        TemporalPointSerializer.serialize(out, value.end)
    }

    override fun deserialize(input: DataInput2, available: Int): TemporalRange = TemporalRange(
            TemporalPointSerializer.deserialize(input, available),
            TemporalPointSerializer.deserialize(input, available)
    )
}