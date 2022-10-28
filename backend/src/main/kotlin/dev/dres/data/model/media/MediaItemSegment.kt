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
    companion object : XdNaturalEntityType<MediaItemSegment>()

    /** The name of this [MediaItemSegment]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [MediaType] of this [MediaItem]. */
    var item by xdParent<MediaItemSegment, MediaItem>(MediaItem::segments)

    /** The start frame number of this [MediaItemSegment]. */
    var startFrame by xdRequiredIntProp() { min(0) }

    /** The end frame number of this [MediaItemSegment]. */
    var endFrame by xdRequiredIntProp() { min(0) }

    /** Returns the [range] of this [MediaItemSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Frame(this.startFrame, this.item.fps ?: 1.0f), TemporalPoint.Frame(this.endFrame, this.item.fps ?: 1.0f))
}