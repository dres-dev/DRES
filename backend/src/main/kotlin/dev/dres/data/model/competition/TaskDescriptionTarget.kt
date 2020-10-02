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
    object JudgementTaskDescriptionTarget : TaskDescriptionTarget() {
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
            return listOf(
                    FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(when(item){
                    is MediaItem.VideoItem -> ContentType.VIDEO
                    is MediaItem.ImageItem -> ContentType.IMAGE
                }, Base64.getEncoder().encodeToString(fileData))
            }
            )
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
            return listOf(FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                ContentElement(ContentType.VIDEO, Base64.getEncoder().encodeToString(fileData))
            })
        }
    }

    /**
     * A [TaskDescriptionTarget], specified by multiple [MediaItem]s.
     */
    data class MultipleMediaItemTarget(val items: List<MediaItem>) : TaskDescriptionTarget() {
        override val ordinal: Int = 4
        override fun textDescription() = "Media Items"
        override fun toQueryContentElement(config: Config, collections: DAO<MediaCollection>): List<ContentElement> {

            val delay = 20L
            var i = 0

            return this.items.map { item ->
                val collection = collections[item.collection]!!
                val file = File(File(collection.basePath), item.location)

                    FileInputStream(file).use { imageInFile ->
                        val fileData = ByteArray(file.length().toInt())
                        imageInFile.read(fileData)
                        ContentElement(when(item){
                            is MediaItem.VideoItem -> ContentType.VIDEO
                            is MediaItem.ImageItem -> ContentType.IMAGE
                        }, Base64.getEncoder().encodeToString(fileData), offset = i++ * delay)
                    }

            }

        }
    }
}