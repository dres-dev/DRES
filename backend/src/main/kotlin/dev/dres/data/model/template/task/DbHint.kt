package dev.dres.data.model.template.task

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.competition.tasks.ApiHint
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

/**
 * Represents the hint given by a [DbTaskTemplate], e.g., a media item or text that is shown.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class DbHint(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbHint>()

    /** The [DbHintType] of this [DbHint]. */
    var type by xdLink1(DbHintType)

    /** The start of a (potential) range. */
    var start by xdNullableLongProp()

    /** The start of a (potential) range. */
    var end by xdNullableLongProp()

    /** The parent [DbTaskTemplate] this [DbHint] belongs to. */
    var task: DbTaskTemplate by xdParent<DbHint,DbTaskTemplate>(DbTaskTemplate::hints)

    /** The[DbMediaItem] shown as part of the [DbHint]. Can be null. */
    var item by xdLink0_1(DbMediaItem)

    /** The target text. Can be null. */
    var text by xdStringProp { requireIf { type == DbHintType.TEXT }}

    /** The target text. Can be null. */
    var path by xdStringProp()

    /** The start of a (potential) range. */
    var temporalRangeStart by xdNullableLongProp { requireIf { type == DbHintType.VIDEO } }

    /** The start of a (potential) range. */
    var temporalRangeEnd by xdNullableLongProp { requireIf { type == DbHintType.VIDEO  } }

    /** Returns the [TemporalRange] of this [DbTaskTemplateTarget]. */
    val range: TemporalRange?
        get() = if (this.temporalRangeStart != null && this.temporalRangeEnd != null) {
            TemporalRange(TemporalPoint.Millisecond(this.temporalRangeStart!!), TemporalPoint.Millisecond(this.temporalRangeEnd!!))
        } else {
            null
        }

    /**
     * Converts this [DbHint] to a RESTful API representation [ApiHint].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiHint]
     */
    fun toApi(): ApiHint = ApiHint(
        type = this.type.toApi(),
        start = this.start,
        end = this.end,
        mediaItem = this.item?.id,
        dataType = this.type.mimeType,
        path = this.path,
        range = this.range?.let { ApiTemporalRange(it) }
    )

    /**
     * Generates and returns a textual description of this [DbHint].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        DbHintType.TEXT -> "\"${this.text}\" from ${this.start ?: "beginning"} to ${end ?: "end"}"
        DbHintType.VIDEO -> {
            if (this.item != null) {
                "Image ${this.item!!.name} from ${start ?: "beginning"} to ${end ?: "end"}"
            } else {
                "Image ${this.path} from ${start ?: "beginning"} to ${end ?: "end"}"
            }
        }
        DbHintType.IMAGE -> {
            if (this.item != null) {
                "Image ${this.item!!.name}"
            } else {
                "Image ${this.path}"
            }
        }
        DbHintType.EMPTY -> "Empty item"
        else -> throw IllegalStateException("The task hint type ${this.type.description} is currently not supported.")
    }
}







