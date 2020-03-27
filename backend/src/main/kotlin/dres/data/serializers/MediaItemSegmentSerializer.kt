package dres.data.serializers

import dres.data.model.basics.MediaItemSegment
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object MediaItemSegmentSerializer: Serializer<MediaItemSegment> {
    override fun serialize(out: DataOutput2, value: MediaItemSegment) {
        out.packLong(value.id)
        out.packLong(value.mediaItemId)
        out.writeUTF(value.name)
        TemporalRangeSerializer.serialize(out, value.range)
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItemSegment {
        return MediaItemSegment(
                input.unpackLong(), input.unpackLong(), input.readUTF(), TemporalRangeSerializer.deserialize(input, available)
        )
    }
}