package dres.api.rest.types.competition.tasks

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.JudgementTaskDescriptionTarget
import dres.data.model.competition.MediaSegmentTarget
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
            is JudgementTaskDescriptionTarget -> RestTaskDescriptionTarget(TaskType.TargetType.JUDGEMENT)
            is MediaSegmentTarget -> RestTaskDescriptionTarget(TaskType.TargetType.SINGLE_MEDIA_SEGMENT, listOf(RestTaskDescriptionTargetItem(target.item.id.toString(), target.temporalRange)))
            else -> throw IllegalStateException("transformation to RestTaskDescriptionTarget from $target not implemented")
        }
    }

    /**
     * Converts this [RestTaskDescriptionTarget] to the corresponding [TaskDescriptionTarget] and returns it.
     *
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toTarget(mediaItems: DAO<MediaItem>) = when(this.type){
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> TODO()
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> MediaSegmentTarget(mediaItems[this.mediaItems.first().mediaItem.UID()]!! as MediaItem.VideoItem, this.mediaItems.first().temporalRange!!)
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TODO()
        TaskType.TargetType.JUDGEMENT -> JudgementTaskDescriptionTarget
    }
}