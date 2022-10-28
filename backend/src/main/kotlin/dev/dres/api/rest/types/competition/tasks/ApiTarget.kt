package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.competition.task.TaskDescriptionTarget
import dev.dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [TaskDescriptionTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class ApiTarget(val type: ApiTargetType, val items: List<ApiTargetItem> = emptyList()) {

    /**
     * Converts this [ApiTarget] to the corresponding [TaskDescriptionTarget] and returns it.
     *
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toTarget(mediaItems: DAO<MediaItem>) = when(this.type){
        TargetOption.SINGLE_MEDIA_SEGMENT -> {
            val item = mediaItems[this.items.first().mediaItem.UID()]!! as MediaItem.VideoItem
            TaskDescriptionTarget.VideoSegmentTarget(item, this.items.first().temporalRange!!.toTemporalRange(item.fps))
        }
        TargetOption.JUDGEMENT -> TaskDescriptionTarget.JudgementTaskDescriptionTarget(
            this.items.map {
                val item = mediaItems[it.mediaItem.UID()]!!
                val fps = if (item is MediaItem.VideoItem) item.fps else 0f
                item to it.temporalRange!!.toTemporalRange(fps)
            }
        )
        TargetOption.SINGLE_MEDIA_ITEM -> TaskDescriptionTarget.MediaItemTarget(mediaItems[this.items.first().mediaItem.UID()]!!)
        TargetOption.MULTIPLE_MEDIA_ITEMS -> TaskDescriptionTarget.MultipleMediaItemTarget(this.items.map { mediaItems[it.mediaItem.UID()]!! })
        TargetOption.VOTE -> TaskDescriptionTarget.VoteTaskDescriptionTarget(
            this.items.map {
                val item = mediaItems[it.mediaItem.UID()]!!
                val fps = if (item is MediaItem.VideoItem) item.fps else 0f
                item to it.temporalRange!!.toTemporalRange(fps)
            }
        )
        TargetOption.TEXT -> TaskDescriptionTarget.TextTaskDescriptionTarget(
            this.items.map { it.mediaItem } //TODO maybe should be renamed from 'mediaItem' to something else
        )
    }
}