package dev.dres.data.serializers

import dev.dres.data.model.basics.time.TemporalPoint
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TemporalPointSerializer: Serializer<TemporalPoint> {
    override fun serialize(out: DataOutput2, value: TemporalPoint) {
        when(value){
            is TemporalPoint.Frame -> {
                out.packInt(1)
                out.packInt(value.frame)
                out.writeFloat(value.fps)
            }
            is TemporalPoint.Millisecond -> {
                out.packInt(2)
                out.packLong(value.millisecond)
            }
            is TemporalPoint.Timecode -> {
                out.packInt(3)
                out.writeUTF(value.timecode)
                out.writeFloat(value.fps)
            }
        }

    }

    override fun deserialize(input: DataInput2, available: Int): TemporalPoint =
        when(input.unpackInt()){
            1 -> TemporalPoint.Frame(input.unpackInt(), input.readFloat())
            2 -> TemporalPoint.Millisecond(input.unpackLong())
            3 -> TemporalPoint.Timecode(input.readUTF(), input.readFloat())
            else -> throw IllegalArgumentException("Unknown type of TemporalPoint")
        }

}