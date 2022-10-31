package dev.dres.data.model.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.min

/**
 * A segment of a [MediaItem] as mostly used by items that exhibit temporal progression.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaItemSegment(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<MediaItemSegment>() {
        /** Combination of [MediaItemSegment] name / item must be unique. */
        override val compositeIndices = listOf(
            listOf(MediaItemSegment::name, MediaItemSegment::item)
        )
    }

    /** The name of this [MediaItemSegment]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [MediaType] of this [MediaItem]. */
    var item by xdParent<MediaItemSegment, MediaItem>(MediaItem::segments)

    /** The start frame number of this [MediaItemSegment]. */
    var start by xdRequiredIntProp { min(0L) }

    /** The end frame number of this [MediaItemSegment]. */
    var end by xdRequiredIntProp { min(0L) }

    /** Returns the [range] of this [MediaItemSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Frame(this.start, this.item.fps ?: 1.0f), TemporalPoint.Frame(this.end, this.item.fps ?: 1.0f))
}