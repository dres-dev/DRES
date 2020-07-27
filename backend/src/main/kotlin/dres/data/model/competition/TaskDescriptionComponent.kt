package dres.data.model.competition

import dres.api.rest.types.competition.tasks.RestTaskDescriptionComponent
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.interfaces.FileTaskDescriptionComponent
import dres.utilities.extensions.UID
import java.io.File

/**
 * Represents a descriptive component of a [TaskDescription], i.e., the part that describes the
 * target of the [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
sealed class TaskDescriptionComponent {
    abstract val start: Long?
    abstract val end: Long?

    /**
     * A textual [TaskDescriptionComponent] consisting of a simple, textual description.
     */
    data class TextTaskDescriptionComponent(val text: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent()

    /**
     * A visual [TaskDescriptionComponent] consisting of a single image  that is part of a collection maintained by DRES.
     */
    data class ImageItemTaskDescriptionComponent(val item: MediaItem.ImageItem, override val start: Long?, override val end: Long?): TaskDescriptionComponent()

    /**
     * A visual [TaskDescriptionComponent] consisting of a segment of a video that is part of a collection maintained by DRES.
     */
    data class VideoItemSegmentTaskDescriptionComponent(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(), CachedVideoItem

    /**
     * A visual [TaskDescriptionComponent] consisting of an external image provided by the user.
     */
    data class ExternalImageTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(), FileTaskDescriptionComponent {
        override fun file(): File = File(imageLocation)
    }

    /*
     * A visual [TaskDescriptionComponent] consisting of an external video provided by the user.
     */
    data class ExternalVideoTaskDescriptionComponent(val videoLocation: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(), FileTaskDescriptionComponent {
        override fun file(): File = File(videoLocation)
    }
}







