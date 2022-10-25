package dev.dres.data.model.basics.media

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.UID
import dev.dres.data.model.basics.time.TemporalPoint
import dev.dres.data.model.basics.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.simple.min
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredIntProp
import kotlinx.dnq.xdRequiredStringProp

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
    var item by xdLink1(MediaItem)

    /** The start frame number of this [MediaItemSegment]. */
    var startFrame by xdRequiredIntProp() { min(0) }

    /** The end frame number of this [MediaItemSegment]. */
    var endFrame by xdRequiredIntProp() { min(0) }

    /** Returns the [range] of this [MediaItemSegment] as [TemporalRange]. */
    val range: TemporalRange
        get() = TemporalRange(TemporalPoint.Frame(this.startFrame, this.item.fps ?: 1.0f), TemporalPoint.Frame(this.endFrame, this.item.fps ?: 1.0f))

}