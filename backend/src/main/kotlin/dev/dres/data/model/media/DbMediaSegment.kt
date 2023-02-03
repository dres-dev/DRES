package dev.dres.data.model.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.min

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
    }

    /** The name of this [DbMediaSegment]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [DbMediaType] of this [DbMediaItem]. */
    var item: DbMediaItem by xdParent<DbMediaSegment, DbMediaItem>(DbMediaItem::segments)

    /** The start frame number of this [DbMediaSegment]. */
    var start by xdRequiredIntProp { min(0L) }

    /** The end frame number of this [DbMediaSegment]. */
    var end by xdRequiredIntProp { min(0L) }

    /** Returns the [range] of this [DbMediaSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Frame(this.start, this.item.fps ?: 1.0f), TemporalPoint.Frame(this.end, this.item.fps ?: 1.0f))
}