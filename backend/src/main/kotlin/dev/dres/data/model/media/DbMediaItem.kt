package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaItemMetaDataEntry
import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.removeAll
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
    override val mediaItemId: MediaItemId
        get() = this.id

    override fun dbCollection(): MediaItemCollection = this.collection

    /** The [DbMediaType] of this [DbMediaItem]. */
    var type by xdLink1(DbMediaType)

    /** The location of this [DbMediaItem] on disk. */
    var location by xdRequiredStringProp(unique = false, trimmed = false)

    /** Frame rate of the [DbMediaItem] in frames per second. Null for type without temporal development. */
    var fps by xdNullableFloatProp() { requireIf { this.type == DbMediaType.VIDEO } }

    /** Duration of the [DbMediaItem] in milliseconds. Null for type without temporal development. */
    var durationMs by xdNullableLongProp() { requireIf { this.type == DbMediaType.VIDEO } }

    /** The [DbMediaCollection] this [DbMediaItem] belongs to. */
    var collection: DbMediaCollection by xdParent<DbMediaItem, DbMediaCollection>(DbMediaCollection::items)
    override fun type(): MediaItemType = MediaItemType.fromDb(this.type)

    /** List of [DbMediaSegment] that this [DbMediaItem] contains.  */
    val segments by xdChildren0_N<DbMediaItem, DbMediaSegment>(DbMediaSegment::item)

    internal val metadata by xdChildren0_N<DbMediaItem, DbMediaItemMetaDataEntry>(DbMediaItemMetaDataEntry::item)

    fun metaDataMap() = metadata.asSequence().map { it.asPair() }.toMap()

    fun getMetaDataValue(key: String): String? = metadata.filter { it.key eq key }.firstOrNull()?.value

    fun setMetaData(key: String, value: String) {
        val existing = metadata.filter { it.key eq key }.firstOrNull()
        if (existing != null) {
            existing.value = value
        } else {
            metadata.add(
                DbMediaItemMetaDataEntry.new {
                    this.key = key
                    this.value = value
                }
            )
        }
    }

    fun setMetaData(metaData: Map<String, String>) = metaData.forEach { (key, value) -> setMetaData(key, value) }

    fun deleteMetaData(key: String) {
        metadata.removeAll(metadata.filter { it.key eq key })
    }

    /**
     * Generates a [ApiMediaItem] this [DbMediaItem] and returns it.
     *
     * @return [ApiMediaItem]
     */
    fun toApi(): ApiMediaItem = ApiMediaItem(
        this.id,
        this.name,
        this.type.toApi(),
        this.collection.id,
        this.location,
        this.durationMs,
        this.fps,
        this.metadata.asSequence().map { ApiMediaItemMetaDataEntry(it.key, it.value) }.toList()
    )

    /**
     * Returns the [Path] to the original file for this [DbMediaItem].
     *
     * @return [Path]
     */
    fun pathToOriginal(): Path = Paths.get(this.collection.path, this.location)
}