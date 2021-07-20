package dev.dres.data.model.competition

import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalRange

interface VideoSegment {

    val item: MediaItem.VideoItem
    val temporalRange: TemporalRange

}

interface CachedItem {

    fun cacheItemName(): String

}

interface CachedVideoItem: VideoSegment, CachedItem {

    override fun cacheItemName(): String = "${item.collection.string}-${item.id.string}-${temporalRange.start.toMilliseconds()}-${temporalRange.end.toMilliseconds()}.mp4"

}