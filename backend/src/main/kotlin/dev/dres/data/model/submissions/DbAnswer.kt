package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiAnswer
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

class DbAnswer(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbAnswer>()

    var answerSet: DbAnswerSet by xdParent<DbAnswer,DbAnswerSet>(DbAnswerSet::answers)

    /** The [DbAnswerType] of this [DbAnswerSet]. */
    var type by xdLink1(DbAnswerType)

    /** The [DbMediaItem] submitted. Only for [DbAnswerType.ITEM] or [DbAnswerType.TEMPORAL]. */
    var item by xdLink0_1(DbMediaItem)

    /** The start frame number of this [DbSubmission]. */
    var start by xdNullableLongProp { requireIf { this.type == DbAnswerType.TEMPORAL } }

    /** The end frame number of this [DbSubmission]. */
    var end by xdNullableLongProp { requireIf { this.type == DbAnswerType.TEMPORAL } }

    /** The text submitted. Only for [DbAnswerType.TEXT] . */
    var text by xdStringProp { requireIf { this.type == DbAnswerType.TEXT } }

    /**  Returns the [TemporalRange] for this [DbAnswerSet]. */
    val temporalRange: TemporalRange?
        get() = try {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } catch (e: NullPointerException) {
            null
        }

    fun toApi() = ApiAnswer(
        type = this.type.toApi(),
        item = this.item?.toApi(),
        start = this.start,
        end = this.end,
        text = this.text
    )
}