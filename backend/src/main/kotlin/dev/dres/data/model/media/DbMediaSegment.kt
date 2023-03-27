package dev.dres.data.model.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import kotlinx.dnq.simple.min
import kotlinx.dnq.util.findById

/**
 * A segment of a [DbMediaItem] as mostly used by items that exhibit temporal progression.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DbMediaSegment(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbMediaSegment>() {
        /** Combination of [DbMediaSegment] name / item must be unique. */
        override val compositeIndices = listOf(
            listOf(DbMediaSegment::name, DbMediaSegment::item)
        )

        fun findContaining(itemId: MediaItemId, time: TemporalPoint): DbMediaSegment? {
            val item = DbMediaItem.filter { it.mediaItemId eq itemId }.firstOrNull() ?: return null
            return findContaining(item, time)
        }

        fun findContaining(item: DbMediaItem, time: TemporalPoint): DbMediaSegment? {
            val ms = time.toMilliseconds().toInt()
            return item.segments.filter { (ms ge it.start) and (it.end ge ms) }.firstOrNull()
        }

    }

    /** The name of this [DbMediaSegment]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [DbMediaType] of this [DbMediaItem]. */
    var item: DbMediaItem by xdParent<DbMediaSegment, DbMediaItem>(DbMediaItem::segments)

    /** The start of this [DbMediaSegment] in milliseconds. */
    var start by xdRequiredIntProp { min(0L) }

    /** The end of this [DbMediaSegment] in milliseconds. */
    var end by xdRequiredIntProp { min(0L) }

    /** Returns the [range] of this [DbMediaSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Millisecond(this.start.toLong()), TemporalPoint.Millisecond(this.end.toLong()))
}