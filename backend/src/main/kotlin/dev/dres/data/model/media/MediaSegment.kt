package dev.dres.data.model.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.XdIdNaturalEntityType
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
class MediaSegment(entity: Entity) : PersistentEntity(entity) {
    companion object : XdIdNaturalEntityType<MediaSegment>() {
        /** Combination of [MediaSegment] name / item must be unique. */
        override val compositeIndices = listOf(
            listOf(MediaSegment::name, MediaSegment::item)
        )
    }

    /** The name of this [MediaSegment]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [MediaType] of this [MediaItem]. */
    var item: MediaItem by xdParent<MediaSegment, MediaItem>(MediaItem::segments)

    /** The start frame number of this [MediaSegment]. */
    var start by xdRequiredIntProp { min(0L) }

    /** The end frame number of this [MediaSegment]. */
    var end by xdRequiredIntProp { min(0L) }

    /** Returns the [range] of this [MediaSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Frame(this.start, this.item.fps ?: 1.0f), TemporalPoint.Frame(this.end, this.item.fps ?: 1.0f))
}