package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiVerdict
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.run.DbTask
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

/**
 * A [DbVerdictStatus] as submitted by a competition participant. Makes a statement about a [DbTask].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbAnswerSet(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbAnswerSet>()

    /** The [DbVerdictStatus] of this [DbAnswerSet]. */
    var status by xdLink1(DbVerdictStatus)

    /** The [DbAnswerType] of this [DbAnswerSet]. */
    var type by xdLink1(DbAnswerType)

    /** The [DbSubmission] this [DbAnswerSet] belongs to. */
    var submission: DbSubmission by xdParent<DbAnswerSet,DbSubmission>(DbSubmission::verdicts)

    /** The [DbTask] this [DbAnswerSet] belongs to. */
    var task: DbTask by xdParent<DbAnswerSet, DbTask>(DbTask::submissions)

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

    /**
     * Converts this [DbVerdictStatus] to a RESTful API representation [ApiVerdict].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiVerdict]
     */
    fun toApi(): ApiVerdict = ApiVerdict(
        status = this.status.toApi(),
        type = this.type.toApi(),
        item = this.item?.toApi(),
        text = this.text,
        start = this.start,
        end = this.end
    )
}