package dres.data.model.basics

import dres.data.model.Entity
import java.nio.file.Path
import java.time.Duration

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class MediaItem : Entity {

    companion object {
        const val VIDEO_MEDIA_ITEM = 0
        const val IMAGE_MEDIA_ITEM = 1
    }

    abstract val collection: Long
    abstract val name: String

    data class ImageItem(override var id: Long, override val name: String, val location: Path, override val collection: Long): MediaItem()

    data class VideoItem(override var id: Long, override val name: String, val location: Path, override val collection: Long, val duration: Duration, val fps: Float): MediaItem()
}