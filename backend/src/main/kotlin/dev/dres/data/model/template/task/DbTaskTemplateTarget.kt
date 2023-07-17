package dev.dres.data.model.template.task

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.template.tasks.ApiTarget
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

/**
 * Represents the target of a [DbTaskTemplate], i.e., the [DbMediaItem] or part thereof that is considered correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class DbTaskTemplateTarget(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTaskTemplateTarget>()

     /** The [DbTargetType] of this [DbTaskTemplateTarget]. */
    var type by xdLink1(DbTargetType)

    /** The parent [DbTaskTemplate] this [DbTaskTemplateTarget] belongs to. */
    var task: DbTaskTemplate by xdParent<DbTaskTemplateTarget,DbTaskTemplate>(DbTaskTemplate::targets)

    /** The targeted  [DbMediaItem]. Can be null. */
    var item by xdLink0_1(DbMediaItem)

    /** The target text. Can be null. */
    var text by xdStringProp() { requireIf { this.type == DbTargetType.TEXT }}

    /** The start of a (potential) range. */
    var start by xdNullableLongProp { requireIf { this.type == DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE } }

    /** The start of a (potential) range. */
    var end by xdNullableLongProp { requireIf { this.type == DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE } }

    /** Returns the [TemporalRange] of this [DbTaskTemplateTarget]. */
    val range: TemporalRange?
        get() = if (this.start != null && this.end != null) {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } else {
            null
        }

    /**
     * Converts this [DbTaskTemplateTarget] to a RESTful API representation [ApiTaskTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTarget]
     */
    fun toApi(): ApiTarget = when(this.type) {
        DbTargetType.JUDGEMENT,
        DbTargetType.JUDGEMENT_WITH_VOTE -> ApiTarget(this.type.toApi(), null)
        DbTargetType.MEDIA_ITEM,
        DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE -> ApiTarget(this.type.toApi(), this.item?.id, this.range?.let { ApiTemporalRange(it) })
        DbTargetType.TEXT -> ApiTarget(this.type.toApi(), this.text)
        else -> throw IllegalStateException("Task description of type ${this.type.description} is not supported.")
    }
}
