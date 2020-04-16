package dres.data.serializers

import dres.data.model.basics.time.TemporalPoint
import dres.data.model.basics.time.TemporalUnit
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TemporalPointSerializer: Serializer<TemporalPoint> {
    override fun serialize(out: DataOutput2, value: TemporalPoint) {
        out.writeDouble(value.value)
        out.writeInt(value.unit.ordinal)
    }

    override fun deserialize(input: DataInput2, available: Int): TemporalPoint = TemporalPoint(
            input.readDouble(),
            TemporalUnit.values()[input.readInt()]
    )
}