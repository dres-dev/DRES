package dev.dres.data.model.competition

import dev.dres.api.rest.types.task.ContentElement
import dev.dres.api.rest.types.task.ContentType
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalRange
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


    companion object {
        const val IMAGE_CONTENT_ELEMENT_DURATION_S = 3L
        const val EMPTY_CONTENT_ELEMENT_DURATION_S = 0L
    }

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
    internal abstract fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement>

    /**
     * A [TaskDescriptionTarget] that is validated by human judges.
     */
    data class JudgementTaskDescriptionTarget(val targets: List<Pair<MediaItem, TemporalRange?>>) : TaskDescriptionTarget() {
        override val ordinal: Int = 1
        override fun textDescription() = "Judgement"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement> = emptyList()
    }

    /**
     * A [TaskDescriptionTarget], specified by a [MediaItem].
     */
    data class MediaItemTarget(val item: MediaItem) : TaskDescriptionTarget() {
        override val ordinal: Int = 2
        override fun textDescription() = "Media Item ${item.name}"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement>  {
            val collection = collections[this.item.collection]!!
            val file = File(File(collection.basePath), this.item.location)
            val contentElements = mutableListOf<ContentElement>()
            FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                when(this.item) {
                    is MediaItem.VideoItem -> {
                        contentElements += ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), 0)
                        contentElements += ContentElement(ContentType.EMPTY, null, Math.floorDiv(this.item.durationMs, 1000L) + 1L)
                    }
                    is MediaItem.ImageItem ->{
                        contentElements += ContentElement(ContentType.IMAGE, Base64.getEncoder().encodeToString(fileData), 0)
                        contentElements += ContentElement(ContentType.EMPTY, null, IMAGE_CONTENT_ELEMENT_DURATION_S)
                    }
                }
            }
            return contentElements
        }
    }

    /**
     * A [TaskDescriptionTarget], specified by a [MediaItem.VideoItem] and a [TemporalRange].
     */
    data class VideoSegmentTarget(override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : TaskDescriptionTarget(), CachedVideoItem {
        override val ordinal: Int = 3
        override fun textDescription() = "Media Item ${item.name} @ ${temporalRange.start.niceText()} - ${temporalRange.end.niceText()}"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement>  {
            val file = File(File(config.cachePath + "/tasks"), this.cacheItemName())
            val contentElements = mutableListOf<ContentElement>()
            FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                contentElements += ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), 0)
                contentElements += ContentElement(ContentType.EMPTY, null, Math.floorDiv(this.temporalRange.durationMs(this.item.fps), 1000L) + 1L)
            }
            return contentElements
        }
    }

    /**
     * A [TaskDescriptionTarget], specified by multiple [MediaItem]s.
     */
    data class MultipleMediaItemTarget(val items: List<MediaItem>) : TaskDescriptionTarget() {
        override val ordinal: Int = 4
        override fun textDescription() = "Media Items"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement> {
            var cummulativeOffset = 0L
            val contentElements = mutableListOf<ContentElement>()
            this.items.forEach { item ->
                val collection = collections[item.collection]!!
                val file = File(File(collection.basePath), item.location)
                FileInputStream(file).use { imageInFile ->
                    val fileData = ByteArray(file.length().toInt())
                    imageInFile.read(fileData)
                    when(item) {
                        is MediaItem.VideoItem -> {
                            contentElements += ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData), cummulativeOffset)
                            cummulativeOffset += Math.floorDiv(item.durationMs, 1000L) + 1L
                        }
                        is MediaItem.ImageItem -> {
                            contentElements += ContentElement(ContentType.IMAGE, Base64.getEncoder().encodeToString(fileData), IMAGE_CONTENT_ELEMENT_DURATION_S)
                            cummulativeOffset += IMAGE_CONTENT_ELEMENT_DURATION_S
                        }
                    }

                    /* Add pause in between. */
                    contentElements += ContentElement(ContentType.EMPTY, null, cummulativeOffset)
                    cummulativeOffset += EMPTY_CONTENT_ELEMENT_DURATION_S
                }
            }
            return contentElements
        }
    }

    data class VoteTaskDescriptionTarget(val targets: List<Pair<MediaItem, TemporalRange?>>) : TaskDescriptionTarget() {
        override val ordinal: Int = 5
        override fun textDescription() = "Judgement with voting"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement> = emptyList()
    }
}