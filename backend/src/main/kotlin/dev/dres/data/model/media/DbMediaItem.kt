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
class DbMediaItem(entity: Entity) : PersistentEntity(entity), MediaItem {
    companion object : XdNaturalEntityType<DbMediaItem>() {
        /** Combination of [DbMediaItem] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(DbMediaItem::name, DbMediaItem::collection)
        )
    }

    /** The name of this [DbMediaItem]. */
    override var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [DbMediaType] of this [DbMediaItem]. */
    var type by xdLink1(DbMediaType)

    /** The location of this [DbMediaItem] on disk. */
    var location by xdRequiredStringProp(unique = false, trimmed = false)

    /** Frame rate of the [DbMediaItem] in frames per second. Null for type without temporal development. */
    var fps by xdNullableFloatProp() { requireIf { this.type == DbMediaType.VIDEO } }

    /** Duration of the [DbMediaItem] in milliseconds. Null for type without temporal development. */
    var durationMs by xdNullableLongProp() { requireIf { this.type ==  DbMediaType.VIDEO } }

    /** The [DbMediaCollection] this [DbMediaItem] belongs to. */
    override var collection: DbMediaCollection by xdParent<DbMediaItem, DbMediaCollection>(DbMediaCollection::items)
    override fun type(): MediaItemType = MediaItemType.fromDb(this.type)

    /** List of [DbMediaSegment] that this [DbMediaItem] contains.  */
    val segments by xdChildren0_N<DbMediaItem, DbMediaSegment>(DbMediaSegment::item)

    /**
     * Generates a [ApiMediaItem] this [DbMediaItem] and returns it.
     *
     * @return [ApiMediaItem]
     */
    fun toApi(): ApiMediaItem
        = ApiMediaItem(this.id, this.name, this.type.toApi(), this.collection.id, this.location, this.durationMs, this.fps)

    /**
     * Returns the [Path] to the original file for this [DbMediaItem].
     *
     * @return [Path]
     */
    fun pathToOriginal(): Path = Paths.get(this.collection.path, this.location)

    /**
     * Returns the [Path] to the cached file for this [DbMediaItem].
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