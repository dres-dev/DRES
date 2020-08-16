package dres.api.rest.types.competition.tasks

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.TaskDescriptionTarget
import dres.data.model.competition.TaskType
import dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [TaskDescriptionTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
data class RestTaskDescriptionTarget(val type: TaskType.TargetType, val mediaItems: List<RestTaskDescriptionTargetItem> = emptyList()) {

    companion object {

        /**
         * Generates a [RestTaskDescriptionTarget] from a [TaskDescriptionTarget] and returns it.
         *
         * @param target The [TaskDescriptionTarget] to convert.
         */
        fun fromTarget(target: TaskDescriptionTarget) = when(target) {
            is TaskDescriptionTarget.JudgementTaskDescriptionTarget -> RestTaskDescriptionTarget(TaskType.TargetType.JUDGEMENT)
            is TaskDescriptionTarget.VideoSegmentTarget -> RestTaskDescriptionTarget(TaskType.TargetType.SINGLE_MEDIA_SEGMENT, listOf(RestTaskDescriptionTargetItem(target.item.id.string, target.temporalRange)))
            is TaskDescriptionTarget.MediaItemTarget -> RestTaskDescriptionTarget(TaskType.TargetType.SINGLE_MEDIA_ITEM, listOf(RestTaskDescriptionTargetItem(target.item.id.string)))
            is TaskDescriptionTarget.MultipleMediaItemTarget -> RestTaskDescriptionTarget(TaskType.TargetType.MULTIPLE_MEDIA_ITEMS, target.items.map { RestTaskDescriptionTargetItem(it.id.string) })
            else -> throw IllegalStateException("transformation to RestTaskDescriptionTarget from $target not implemented")
        }
    }

    /**
     * Converts this [RestTaskDescriptionTarget] to the corresponding [TaskDescriptionTarget] and returns it.
     *
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toTarget(mediaItems: DAO<MediaItem>) = when(this.type){
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TaskDescriptionTarget.VideoSegmentTarget(mediaItems[this.mediaItems.first().mediaItem.UID()]!! as MediaItem.VideoItem, this.mediaItems.first().temporalRange!!)
        TaskType.TargetType.JUDGEMENT -> TaskDescriptionTarget.JudgementTaskDescriptionTarget
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> TaskDescriptionTarget.MediaItemTarget(mediaItems[this.mediaItems.first().mediaItem.UID()]!!)
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TaskDescriptionTarget.MultipleMediaItemTarget(this.mediaItems.map { mediaItems[it.mediaItem.UID()]!! })
    }
}