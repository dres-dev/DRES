package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiVerdict
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.run.Task
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf

/**
 * A [VerdictStatus] as submitted by a competition participant. Makes a statement about a [Task].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class Verdict(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<Verdict>()

    /** The [VerdictStatus] of this [Verdict]. */
    var status by xdLink1(VerdictStatus)

    /** The [VerdictType] of this [Verdict]. */
    var type by xdLink1(VerdictType)

    /** The [Submission] this [Verdict] belongs to. */
    var submission: Submission by xdParent<Verdict,Submission>(Submission::verdicts)

    /** The [Task] this [Verdict] belongs to. */
    var task: Task by xdParent<Verdict, Task>(Task::submissions)

    /** The [MediaItem] submitted. Only for [VerdictType.ITEM] or [VerdictType.TEMPORAL]. */
    var item by xdLink0_1(MediaItem)

    /** The start frame number of this [Submission]. */
    var start by xdNullableLongProp { requireIf { this.type == VerdictType.TEMPORAL } }

    /** The end frame number of this [Submission]. */
    var end by xdNullableLongProp { requireIf { this.type == VerdictType.TEMPORAL } }

    /** The text submitted. Only for [VerdictType.TEXT] . */
    var text by xdStringProp { requireIf { this.type == VerdictType.TEXT } }

    /**  Returns the [TemporalRange] for this [Verdict]. */
    val temporalRange: TemporalRange?
        get() = try {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } catch (e: NullPointerException) {
            null
        }

    /**
     * Converts this [VerdictStatus] to a RESTful API representation [ApiVerdict].
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