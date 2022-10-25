package dev.dres.data.model.basics.media

import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A media item such as a video or an image
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaItem(entity: Entity) : PersistentEntity(entity) {

    /** */
    companion object : XdNaturalEntityType<MediaItem>()

    /** The name of this [MediaItem]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The location of this [MediaItem] on disk. */
    var location by xdRequiredStringProp(unique = false, trimmed = false)

    /** Frame rate of the [MediaItem] in frames per second. Null for type without temporal development. */
    var fps by xdNullableFloatProp()

    /** Duration of the [MediaItem] in milliseconds. Null for type without temporal development. */
    var durationMs by xdNullableLongProp()

    /** The [MediaType] of this [MediaItem]. */
    var type by xdLink1(MediaType)

    /** The [MediaCollection] this [MediaItem] belongs to. */
    var collection by xdLink1(MediaCollection)

    /** List of [MediaItemSegment] that this [MediaItem] contains.  */
    val segments by xdLink0_N(MediaItemSegment::item)
}