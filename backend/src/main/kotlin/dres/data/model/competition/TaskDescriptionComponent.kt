package dres.data.model.competition

import dres.api.rest.types.task.ContentType
import dres.api.rest.types.task.ContentElement
import dres.data.model.Config
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.interfaces.FileTaskDescriptionComponent
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * Represents a descriptive component of a [TaskDescription], i.e., the part that describes the
 * target of the [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
 */
sealed class TaskDescriptionComponent(internal val contentType: ContentType) {
    abstract val start: Long?
    abstract val end: Long?

    internal abstract fun textDescription(): String

    /**
     * Generates and returns a [ContentElement] object to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [ContentElement]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    internal abstract fun toQueryContentElement(config: Config): ContentElement

    /**
     * A textual [TaskDescriptionComponent] consisting of a simple, textual description.
     */
    data class TextTaskDescriptionComponent(val text: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(ContentType.TEXT) {
        override fun textDescription(): String = "\"$text\" from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement = ContentElement(ContentType.TEXT, this.text, this.start ?: 0)
    }

    /**
     * A visual [TaskDescriptionComponent] consisting of a single image  that is part of a collection maintained by DRES.
     */
    data class ImageItemTaskDescriptionComponent(val item: MediaItem.ImageItem, override val start: Long?, override val end: Long?): TaskDescriptionComponent(ContentType.IMAGE) {
        override fun textDescription(): String = "Image ${item.name} from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement {
            val file = File(config.cachePath + "/tasks")
            return FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.IMAGE, Base64.getEncoder().encodeToString(fileData), this.start ?: 0)
            }
        }
    }

    /**
     * A visual [TaskDescriptionComponent] consisting of a segment of a video that is part of a collection maintained by DRES.
     */
    data class VideoItemSegmentTaskDescriptionComponent(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(ContentType.VIDEO), CachedVideoItem {
        override fun textDescription(): String = "Video ${item.name} from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement {
            val file = File(config.cachePath + "/tasks", this.cacheItemName())
            return FileInputStream(file).let { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), this.start ?: 0)
            }
        }
    }

    /**
     * A visual [TaskDescriptionComponent] consisting of an external image provided by the user.
     */
    data class ExternalImageTaskDescriptionComponent(val imageLocation: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(ContentType.IMAGE), FileTaskDescriptionComponent {
        override fun file(): File = File(imageLocation)
        override fun textDescription(): String = "External Image at $imageLocation from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement = TODO()
    }

    /*
     * A visual [TaskDescriptionComponent] consisting of an external video provided by the user.
     */
    data class ExternalVideoTaskDescriptionComponent(val videoLocation: String, override val start: Long?, override val end: Long?) : TaskDescriptionComponent(ContentType.VIDEO), FileTaskDescriptionComponent {
        override fun file(): File = File(videoLocation)
        override fun textDescription(): String = "External Video at $videoLocation from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement = TODO()
    }
}







