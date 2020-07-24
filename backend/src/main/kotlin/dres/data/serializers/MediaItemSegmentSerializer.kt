package dres.data.serializers

import dres.data.model.basics.media.MediaItemSegment
import dres.data.model.basics.media.MediaItemSegmentList
import dres.utilities.extensions.readUID
import dres.utilities.extensions.writeUID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object MediaItemSegmentListSerializer: Serializer<MediaItemSegmentList> {

    override fun serialize(out: DataOutput2, value: MediaItemSegmentList) {
        out.writeUID(value.id)
        out.writeUID(value.mediaItemId)
        out.packInt(value.segments.size)
        value.segments.forEach {
            out.writeUTF(it.name)
            TemporalRangeSerializer.serialize(out, it.range)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): MediaItemSegmentList {
        val id = input.readUID()
        val mediaItemId = input.readUID()
        val segments = (0 until input.unpackInt()).map {MediaItemSegment(mediaItemId, input.readUTF(), TemporalRangeSerializer.deserialize(input, available))}.toMutableList()
        return MediaItemSegmentList(id, mediaItemId, segments)
    }

}