package dres.data.model.competition

import dres.api.rest.types.task.ContentType
import dres.api.rest.types.task.ContentElement
import dres.data.model.Config
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


/**
 * Represents the target of a [TaskDescription], i.e., the media object or segment that is
 * considered correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
 */
sealed class TaskDescriptionTarget {

    abstract val ordinal: Int

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
    internal abstract fun toQueryContentElement(config: Config): ContentElement?

    /**
     * A [TaskDescriptionTarget] that is validated by human judges.
     */
    object JudgementTaskDescriptionTarget : TaskDescriptionTarget() {
        override val ordinal: Int = 1
        override fun textDescription() = "Judgement"
        override fun toQueryContentElement(config: Config): ContentElement? = null
    }

    /**
     * A [TaskDescriptionTarget], specified by a [MediaItem].
     */
    data class MediaItemTarget(val item: MediaItem) : TaskDescriptionTarget() {
        override val ordinal: Int = 2
        override fun textDescription() = "Media Item ${item.name}"
        override fun toQueryContentElement(config: Config): ContentElement? = TODO()
    }

    /**
     * A [TaskDescriptionTarget], specified by a [MediaItem.VideoItem] and a [TemporalRange].
     */
    data class VideoSegmentTarget(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : TaskDescriptionTarget(), CachedVideoItem {
        override val ordinal: Int = 3
        override fun textDescription() = "Media Item ${item.name} @ ${temporalRange.start.niceText()} - ${temporalRange.end.niceText()}"
        override fun toQueryContentElement(config: Config): ContentElement? {
            val file = File(File(config.cachePath + "/tasks"), this.cacheItemName())
            return FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData))
            }
        }
    }

    /**
     * A [TaskDescriptionTarget], specified by multiple [MediaItem]s.
     */
    data class MultipleMediaItemTarget(val items: List<MediaItem>) : TaskDescriptionTarget() {
        override val ordinal: Int = 4
        override fun textDescription() = "Media Items"
        override fun toQueryContentElement(config: Config): ContentElement? = TODO()
    }
}