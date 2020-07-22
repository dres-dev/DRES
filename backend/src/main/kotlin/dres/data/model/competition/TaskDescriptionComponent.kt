package dres.data.model.competition

import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import java.io.File

interface TaskDescriptionComponent {

    val start: Long?
    val end: Long?

}

interface CachedTaskDescriptionComponent : TaskDescriptionComponent, CachedItem
interface FileTaskDescriptionComponent : TaskDescriptionComponent{

    fun file(): File

}


data class TextTaskDescriptionComponent(val text: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent

data class ExternalImageTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : FileTaskDescriptionComponent {
    override fun file(): File = File(imageLocation)
}

data class ExternalVideoTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : FileTaskDescriptionComponent {
    override fun file(): File = File(imageLocation)
}

data class VideoItemSegmentTaskDescriptionComponent(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, override val start: Long?, override val end: Long?) : CachedVideoItem, CachedTaskDescriptionComponent