package dres.data.model.competition

import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange

interface TaskDescriptionTarget

data class MediaSegmentTarget(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : TaskDescriptionTarget, CachedVideoItem