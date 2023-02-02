package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf
import java.nio.file.Path
import java.nio.file.Paths

typealias MediaId = String

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaItem(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<MediaItem>() {
        /** Combination of [MediaItem] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(MediaItem::name, MediaItem::collection)
        )
    }

    /** The name of this [MediaItem]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [MediaType] of this [MediaItem]. */
    var type by xdLink1(MediaType)

    /** The location of this [MediaItem] on disk. */
    var location by xdRequiredStringProp(unique = false, trimmed = false)

    /** Frame rate of the [MediaItem] in frames per second. Null for type without temporal development. */
    var fps by xdNullableFloatProp() { requireIf { this.type == MediaType.VIDEO } }

    /** Duration of the [MediaItem] in milliseconds. Null for type without temporal development. */
    var durationMs by xdNullableLongProp() { requireIf { this.type ==  MediaType.VIDEO } }

    /** The [MediaCollection] this [MediaItem] belongs to. */
    var collection: MediaCollection by xdParent<MediaItem, MediaCollection>(MediaCollection::items)

    /** List of [MediaSegment] that this [MediaItem] contains.  */
    val segments by xdChildren0_N<MediaItem, MediaSegment>(MediaSegment::item)

    /**
     * Generates a [ApiMediaItem] this [MediaItem] and returns it.
     *
     * @return [ApiMediaItem]
     */
    fun toApi(): ApiMediaItem
        = ApiMediaItem(this.id, this.name, this.type.toApi(), this.collection.id, this.location, this.durationMs, this.fps)

    /**
     * Returns the [Path] to the original file for this [MediaItem].
     *
     * @return [Path]
     */
    fun pathToOriginal(): Path = Paths.get(this.collection.path, this.location)

    /**
     * Returns the [Path] to the cached file for this [MediaItem].
     *
     * @param start
     * @param end
     * @return [Path]
     */
    fun cachedItemName(start: Long? = null, end: Long? = null) = if (start != null && end != null) {
        "${this.collection.name}-${this.id}-$start-$end.${this.type.suffix}"
    }  else {
        "${this.collection.name}-${this.id}.${this.type.suffix}"
    }
}