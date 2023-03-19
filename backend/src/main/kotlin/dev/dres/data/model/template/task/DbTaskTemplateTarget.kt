package dev.dres.data.model.template.task

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.competition.tasks.ApiTarget
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
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
    var text by xdStringProp() { requireIf { item == null }}

    /** The start of a (potential) range. */
    var start by xdNullableLongProp { requireIf { item?.type == DbMediaType.VIDEO } }

    /** The start of a (potential) range. */
    var end by xdNullableLongProp { requireIf { item?.type == DbMediaType.VIDEO } }

    /** Returns the [TemporalRange] of this [DbTaskTemplateTarget]. */
    val range: TemporalRange?
        get() = if (this.start != null && this.end != null) {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } else {
            null
        }

    /**
     * Generates and returns a textual description of this [DbTaskTemplateTarget].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        DbTargetType.JUDGEMENT -> "Judgement"
        DbTargetType.JUDGEMENT_WITH_VOTE -> "Judgement with vote"
        DbTargetType.MEDIA_ITEM -> "Media item ${this.item?.name}"
        DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE -> "Media item ${this.item?.name} @ ${this.start} - ${this.end}"
        DbTargetType.TEXT -> "Text: ${this.text}"
        else -> throw IllegalStateException("The task description type ${this.type.description} is currently not supported.")
    }

    /**
     *
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