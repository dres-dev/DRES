package dev.dres.data.model.template.task

import dev.dres.DRES
import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.competition.tasks.ApiHint
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.Config
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.FilteringContext.le
import kotlinx.dnq.simple.requireIf
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

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

    /**
     * Generates and returns a [ApiContentElement] object of this [DbHint] to be used by the RESTful interface.
     *
     * @return [ApiContentElement]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toQueryContentElement(): ApiContentElement {
        val cacheLocation: Path = DRES.CACHE_ROOT.resolve("tasks")
        val content = when (this.type) {
            DbHintType.IMAGE -> {
                val filePath = this.item?.pathToOriginal() ?: this.path?.let { Paths.get(it) }
                if (filePath != null && Files.exists(filePath)) {
                    Base64.getEncoder().encodeToString(Files.readAllBytes(filePath))
                } else {
                    null
                }
            }
            DbHintType.VIDEO -> {
                val filePath = this.item?.cachedItemName(this.temporalRangeStart, this.temporalRangeEnd)?.let { cacheLocation.resolve(it) } ?: this.path?.let { Paths.get(it) }
                if (Files.exists(filePath)) {
                    Base64.getEncoder().encodeToString(Files.readAllBytes(filePath))
                } else {
                    null
                }
            }
            DbHintType.EMPTY -> ""
            DbHintType.TEXT -> this.text ?: throw IllegalStateException("A hint of type  ${this.type.description} must have a valid text.")
            else -> throw IllegalStateException("The hint type ${this.type.description} is not supported.")
        }

        val contentType = when (this.type) {
            DbHintType.IMAGE -> ApiContentType.IMAGE
            DbHintType.VIDEO -> ApiContentType.VIDEO
            DbHintType.TEXT -> ApiContentType.TEXT
            DbHintType.EMPTY -> ApiContentType.EMPTY
            else ->  throw IllegalStateException("The hint type ${this.type.description} is not supported.")
        }

        return ApiContentElement(contentType = contentType, content = content, offset = this.start ?: 0L)
    }
}







