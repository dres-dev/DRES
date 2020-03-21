package dres.data.model.basics

import java.nio.file.Path

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class MediaItem(val id: Long, val name: String, val location: Path)

class ImageItem(id: Long, name: String, location: Path): MediaItem(id, name, location)

class VideoItem(id: Long, name: String, location: Path, val range: TemporalRange): MediaItem(id, name, location)