package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.dbo.DAO
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.competition.options.TargetOption
import dev.dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [TaskDescriptionTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class RestTaskDescriptionTarget(val type: TargetOption, val mediaItems: List<RestTaskDescriptionTargetItem> = emptyList()) {

    companion object {

        /**
         * Generates a [RestTaskDescriptionTarget] from a [TaskDescriptionTarget] and returns it.
         *
         * @param target The [TaskDescriptionTarget] to convert.
         */
        fun fromTarget(target: TaskDescriptionTarget) = when(target) {
            is TaskDescriptionTarget.JudgementTaskDescriptionTarget -> RestTaskDescriptionTarget(TargetOption.JUDGEMENT, target.targets.map { RestTaskDescriptionTargetItem(it.first.id.string, if (it.second == null) null else RestTemporalRange(it.second!!)) })
            is TaskDescriptionTarget.VideoSegmentTarget -> RestTaskDescriptionTarget(TargetOption.SINGLE_MEDIA_SEGMENT, listOf(RestTaskDescriptionTargetItem(target.item.id.string, RestTemporalRange(target.temporalRange))))
            is TaskDescriptionTarget.MediaItemTarget -> RestTaskDescriptionTarget(TargetOption.SINGLE_MEDIA_ITEM, listOf(RestTaskDescriptionTargetItem(target.item.id.string)))
            is TaskDescriptionTarget.MultipleMediaItemTarget -> RestTaskDescriptionTarget(TargetOption.MULTIPLE_MEDIA_ITEMS, target.items.map { RestTaskDescriptionTargetItem(it.id.string) })
            is TaskDescriptionTarget.VoteTaskDescriptionTarget -> RestTaskDescriptionTarget(TargetOption.VOTE, target.targets.map { RestTaskDescriptionTargetItem(it.first.id.string, if (it.second == null) null else RestTemporalRange(it.second!!)) })
        }
    }

    /**
     * Converts this [RestTaskDescriptionTarget] to the corresponding [TaskDescriptionTarget] and returns it.
     *
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toTarget(mediaItems: DAO<MediaItem>) = when(this.type){
        TargetOption.SINGLE_MEDIA_SEGMENT -> {
            val item = mediaItems[this.mediaItems.first().mediaItem.UID()]!! as MediaItem.VideoItem
            TaskDescriptionTarget.VideoSegmentTarget(item, this.mediaItems.first().temporalRange!!.toTemporalRange(item.fps))
        }
        TargetOption.JUDGEMENT -> TaskDescriptionTarget.JudgementTaskDescriptionTarget(
            this.mediaItems.map {
                val item = mediaItems[it.mediaItem.UID()]!!
                val fps = if (item is MediaItem.VideoItem) item.fps else 0f
                item to it.temporalRange!!.toTemporalRange(fps)
            }
        )
        TargetOption.SINGLE_MEDIA_ITEM -> TaskDescriptionTarget.MediaItemTarget(mediaItems[this.mediaItems.first().mediaItem.UID()]!!)
        TargetOption.MULTIPLE_MEDIA_ITEMS -> TaskDescriptionTarget.MultipleMediaItemTarget(this.mediaItems.map { mediaItems[it.mediaItem.UID()]!! })
        TargetOption.VOTE -> TaskDescriptionTarget.VoteTaskDescriptionTarget(
            this.mediaItems.map {
                val item = mediaItems[it.mediaItem.UID()]!!
                val fps = if (item is MediaItem.VideoItem) item.fps else 0f
                item to it.temporalRange!!.toTemporalRange(fps)
            }
        )
    }
}