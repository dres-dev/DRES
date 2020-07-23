package dres.data.model.competition

import dres.api.rest.types.RestTaskDescriptionComponent
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import java.io.File

interface TaskDescriptionComponent {

    val start: Long?
    val end: Long?

}

fun TaskDescriptionComponent(component: RestTaskDescriptionComponent, mediaItems: DAO<MediaItem>): TaskDescriptionComponent =
    when(component.type){
        TaskType.QueryComponentType.IMAGE_ITEM -> ImageItemTaskDescriptionComponent(mediaItems[component.mediaItem!!.toLong()] as MediaItem.ImageItem, component.start, component.end)
        TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT -> VideoItemSegmentTaskDescriptionComponent(mediaItems[component.mediaItem!!.toLong()] as MediaItem.VideoItem, component.range!!, component.start, component.end)
        TaskType.QueryComponentType.TEXT -> TextTaskDescriptionComponent(component.description ?: "", component.start, component.end)
        TaskType.QueryComponentType.EXTERNAL_IMAGE -> TODO()
        TaskType.QueryComponentType.EXTERNAL_VIDEO -> TODO()
    }


interface CachedTaskDescriptionComponent : TaskDescriptionComponent, CachedItem

interface FileTaskDescriptionComponent : TaskDescriptionComponent{

    fun file(): File

}


data class TextTaskDescriptionComponent(val text: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent

data class ExternalImageTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : FileTaskDescriptionComponent {
    override fun file(): File = File(imageLocation)
}

data class ImageItemTaskDescriptionComponent(val item: MediaItem.ImageItem, override val start: Long?, override val end: Long?): TaskDescriptionComponent

data class ExternalVideoTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : FileTaskDescriptionComponent {
    override fun file(): File = File(imageLocation)
}

data class VideoItemSegmentTaskDescriptionComponent(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, override val start: Long?, override val end: Long?) : CachedVideoItem, CachedTaskDescriptionComponent