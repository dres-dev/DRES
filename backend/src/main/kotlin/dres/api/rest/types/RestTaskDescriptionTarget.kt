package dres.api.rest.types

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.JudgementTaskDescriptionTarget
import dres.data.model.competition.MediaSegmentTarget
import dres.data.model.competition.TaskDescriptionTarget
import dres.data.model.competition.TaskType
import dres.utilities.extensions.UID

data class RestTaskDescriptionTarget(
        val type: TaskType.TargetType,
        val mediaItems: List<String> = emptyList(),
        val range: TemporalRange? = null
)

fun RestTaskDescriptionTarget(target: TaskDescriptionTarget) : RestTaskDescriptionTarget {

    return when(target) {
        is JudgementTaskDescriptionTarget -> RestTaskDescriptionTarget(TaskType.TargetType.JUDGEMENT)
        is MediaSegmentTarget -> RestTaskDescriptionTarget(TaskType.TargetType.SINGLE_MEDIA_SEGMENT, listOf(target.item.id.toString()), target.temporalRange)
        else -> throw IllegalStateException("transformation to RestTaskDescriptionTarget from $target not implemented")
    }

}

fun TaskDescriptionTarget(target: RestTaskDescriptionTarget, mediaItems: DAO<MediaItem>): TaskDescriptionTarget = when(target.type){
    TaskType.TargetType.SINGLE_MEDIA_ITEM -> TODO()
    TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> MediaSegmentTarget(mediaItems[target.mediaItems.first().UID()]!! as MediaItem.VideoItem, target.range!!)
    TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TODO()
    TaskType.TargetType.JUDGEMENT -> JudgementTaskDescriptionTarget
}