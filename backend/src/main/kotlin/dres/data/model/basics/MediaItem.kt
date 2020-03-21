package dres.data.model.basics

import java.nio.file.Path

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class MediaItem() {

    companion object {
        const val VIDEO_MEDIA_ITEM = 0
        const val IMAGE_MEDIA_ITEM = 1
    }

    class ImageItem(val id: Long, val name: String, val location: Path): MediaItem()

    class VideoItem(val id: Long, val name: String, val location: Path, val range: TemporalRange): MediaItem()
}