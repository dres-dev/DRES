package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiAnswer
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

class DbAnswer(entity: Entity) : PersistentEntity(entity), Answer {
    companion object : XdNaturalEntityType<DbAnswer>()

    var answerSet: DbAnswerSet by xdParent<DbAnswer,DbAnswerSet>(DbAnswerSet::answers)

    /** The [DbAnswerType] of this [DbAnswerSet]. */
    var type by xdLink1(DbAnswerType)

    /** The [DbMediaItem] submitted. Only for [DbAnswerType.ITEM] or [DbAnswerType.TEMPORAL]. */
    override var item by xdLink0_1(DbMediaItem)

    /** The start frame number of this [DbSubmission]. */
    override var start by xdNullableLongProp { requireIf { this.type == DbAnswerType.TEMPORAL } }

    /** The end frame number of this [DbSubmission]. */
    override var end by xdNullableLongProp { requireIf { this.type == DbAnswerType.TEMPORAL } }

    /** The text submitted. Only for [DbAnswerType.TEXT] . */
    override var text by xdStringProp { requireIf { this.type == DbAnswerType.TEXT } }

    override fun type(): AnswerType = AnswerType.fromDb(this.type)

    /**
     * Converts this [DbAnswer] to a RESTful API representation [ApiAnswer].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @param blind True, if a "blind" [ApiAnswer] should be generated.
     * @return [ApiAnswer]
     */
    fun toApi(blind: Boolean = false) = ApiAnswer(
        type = this.type().toApi(),
        item = if (blind) { null } else { this.item?.toApi() },
        start = if (blind) { null } else { this.start },
        end = if (blind) { null } else { this.end },
        text = if (blind) { null } else { this.text }
    )
}
