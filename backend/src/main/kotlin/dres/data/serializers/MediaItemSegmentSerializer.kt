package dres.data.serializers

import dres.data.model.basics.media.MediaItemSegment
import dres.data.model.basics.media.MediaItemSegmentList
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object MediaItemSegmentListSerializer: Serializer<MediaItemSegmentList> {

    override fun serialize(out: DataOutput2, value: MediaItemSegmentList) {
        out.packLong(value.id)
        out.packLong(value.mediaItemId)
        out.packInt(value.segments.size)
        value.segments.forEach {
            out.writeUTF(it.name)
            TemporalRangeSerializer.serialize(out, it.range)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItemSegmentList {
        val id = input.unpackLong()
        val mediaItemId = input.unpackLong()
        val segments = (0 until input.unpackInt()).map {MediaItemSegment(mediaItemId, input.readUTF(), TemporalRangeSerializer.deserialize(input, available))}.toMutableList()
        return MediaItemSegmentList(id, mediaItemId, segments)
    }

}