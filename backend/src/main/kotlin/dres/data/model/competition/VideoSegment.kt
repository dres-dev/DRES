package dres.data.model.competition

import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange

interface VideoSegment {

    val item: MediaItem.VideoItem
    val temporalRange: TemporalRange

}

interface CachedItem {

    fun cacheItemName(): String

}

interface CachedVideoItem: VideoSegment, CachedItem {

    override fun cacheItemName(): String = "${item.collection}-${item.id}-${temporalRange.start.value}-${temporalRange.end.value}.mp4"

}