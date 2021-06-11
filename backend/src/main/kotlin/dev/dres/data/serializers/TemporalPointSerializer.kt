package dev.dres.data.serializers

import dev.dres.data.model.basics.time.FrameTemporalPoint
import dev.dres.data.model.basics.time.MilliSecondTemporalPoint
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.basics.time.TimeCodeTemporalPoint
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TemporalPointSerializer: Serializer<TemporalPoint> {
    override fun serialize(out: DataOutput2, value: TemporalPoint) {
        when(value){
            is FrameTemporalPoint -> {
                out.packInt(1)
                out.packInt(value.frame)
                out.writeFloat(value.framesPerSecond)
            }
            is MilliSecondTemporalPoint -> {
                out.packInt(2)
                out.packLong(value.millisecond)
            }
            is TimeCodeTemporalPoint -> {
                out.packInt(3)
                out.writeUTF(value.timecode)
                out.writeFloat(value.framesPerSecond)
            }
        }

    }

    override fun deserialize(input: DataInput2, available: Int): TemporalPoint =
        when(input.unpackInt()){
            1 -> FrameTemporalPoint(input.unpackInt(), input.readFloat())
            2 -> MilliSecondTemporalPoint(input.unpackLong())
            3 -> TimeCodeTemporalPoint(input.readUTF(), input.readFloat())
            else -> throw IllegalArgumentException("Unknown type of TemporalPoint")
        }

}