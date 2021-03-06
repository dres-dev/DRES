package dev.dres.data.model.competition

import dev.dres.api.rest.types.task.ContentType
import dev.dres.api.rest.types.task.ContentElement
import dev.dres.data.model.Config
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalRange
import dev.dres.data.model.competition.interfaces.FileTaskDescriptionComponent
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Represents a descriptive component of a [TaskDescription], i.e., the part that describes the
 * target of the [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.2
 */
sealed class TaskDescriptionHint(internal val contentType: ContentType) {
    abstract val ordinal: Int
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
     * A textual [TaskDescriptionHint] consisting of a simple, textual description.
     */
    data class TextTaskDescriptionHint(val text: String, override val start: Long?, override val end: Long?) : TaskDescriptionHint(ContentType.TEXT) {
        override val ordinal = 1
        override fun textDescription(): String = "\"$text\" from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement = ContentElement(ContentType.TEXT, this.text, this.start ?: 0)
    }

    /**
     * A visual [TaskDescriptionHint] consisting of a single image  that is part of a collection maintained by DRES.
     */
    data class ImageItemTaskDescriptionHint(val item: MediaItem.ImageItem, override val start: Long?, override val end: Long?): TaskDescriptionHint(ContentType.IMAGE) {
        override val ordinal = 2
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
     * A visual [TaskDescriptionHint] consisting of a segment of a video that is part of a collection maintained by DRES.
     */
    data class VideoItemSegmentTaskDescriptionHint(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, override val start: Long?, override val end: Long?) : TaskDescriptionHint(ContentType.VIDEO), CachedVideoItem {
        override val ordinal = 3
        override fun textDescription(): String = "Video ${item.name} from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement {
            val file = File(config.cachePath + "/tasks", this.cacheItemName())
            return FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), this.start ?: 0)
            }
        }
    }

    /**
     * A visual [TaskDescriptionHint] consisting of an external image provided by the user.
     */
    data class ExternalImageTaskDescriptionHint(val imageLocation: Path, override val start: Long?, override val end: Long?) : TaskDescriptionHint(ContentType.IMAGE) {
        override val ordinal = 4
        override fun textDescription(): String = "External Image at $imageLocation from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement {
            return Files.newInputStream(this.imageLocation).use { imageInFile ->
                val fileData = ByteArray(Files.size(this.imageLocation).toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.IMAGE, Base64.getEncoder().encodeToString(fileData), this.start ?: 0)
            }
        }
    }

    /*
     * A visual [TaskDescriptionComponent] consisting of an external video provided by the user.
     */
    data class ExternalVideoTaskDescriptionHint(val videoLocation: Path, override val start: Long?, override val end: Long?) : TaskDescriptionHint(ContentType.VIDEO) {
        override val ordinal = 5
        override fun textDescription(): String = "External Video at $videoLocation from ${start ?: "beginning"} to ${end ?: "end"}"
        override fun toQueryContentElement(config: Config): ContentElement {
            return Files.newInputStream(this.videoLocation).use { imageInFile ->
                val fileData = ByteArray(Files.size(videoLocation).toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), this.start ?: 0)
            }
        }
    }
}







