package dev.dres.data.model.template.task

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.competition.tasks.ApiHint
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.Config
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.min
import kotlinx.dnq.simple.requireIf
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Represents the hint given by a [TaskTemplate], e.g., a media item or text that is shown.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class Hint(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<Hint>()

    /** The [HintType] of this [Hint]. */
    var type by xdLink1(HintType)

    /** The start of a (potential) range. */
    var start by xdNullableLongProp()

    /** The start of a (potential) range. */
    var end by xdNullableLongProp()

    /** The parent [TaskTemplate] this [Hint] belongs to. */
    var task: TaskTemplate by xdParent<Hint,TaskTemplate>(TaskTemplate::hints)

    /** The[MediaItem] shown as part of the [Hint]. Can be null. */
    var item by xdLink0_1(MediaItem)

    /** The target text. Can be null. */
    var text by xdStringProp() { requireIf { type == HintType.TEXT }}

    /** The target text. Can be null. */
    var path by xdStringProp()

    /** The start of a (potential) range. */
    var temporalRangeStart by xdNullableLongProp { requireIf { type == HintType.VIDEO } }

    /** The start of a (potential) range. */
    var temporalRangeEnd by xdNullableLongProp { requireIf { type == HintType.VIDEO  } }

    /** Returns the [TemporalRange] of this [TaskTemplateTarget]. */
    val range: TemporalRange?
        get() = if (this.temporalRangeStart != null && this.temporalRangeEnd != null) {
            TemporalRange(TemporalPoint.Millisecond(this.temporalRangeStart!!), TemporalPoint.Millisecond(this.temporalRangeEnd!!))
        } else {
            null
        }


    /**
     * Converts this [Hint] to a RESTful API representation [ApiHint].
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
     * Generates and returns a textual description of this [Hint].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        HintType.TEXT -> "\"${this.text}\" from ${this.start ?: "beginning"} to ${end ?: "end"}"
        HintType.VIDEO -> {
            if (this.item != null) {
                "Image ${this.item!!.name} from ${start ?: "beginning"} to ${end ?: "end"}"
            } else {
                "Image ${this.path} from ${start ?: "beginning"} to ${end ?: "end"}"
            }
        }
        HintType.IMAGE -> {
            if (this.item != null) {
                "Image ${this.item!!.name}"
            } else {
                "Image ${this.path}"
            }
        }
        HintType.EMPTY -> "Empty item"
        else -> throw IllegalStateException("The task hint type ${this.type.description} is currently not supported.")
    }

    /**
     * Generates and returns a [ApiContentElement] object of this [Hint] to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [ApiContentElement]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toQueryContentElement(config: Config): ApiContentElement {
        val content = when (this.type) {
            HintType.IMAGE,
            HintType.VIDEO -> {
                val path = if (this.item != null) {
                    Paths.get(config.cachePath, this.item!!.cachedItemName(this.temporalRangeStart, this.temporalRangeEnd))
                } else if (this.path != null) {
                    Paths.get(this.path!!)
                } else {
                    throw IllegalArgumentException("A hint of type  ${this.type.description} must have a valid media item or external path.")
                }
                val data = Files.newInputStream(path, StandardOpenOption.READ).use { stream ->
                    stream.readAllBytes()
                }
                Base64.getEncoder().encodeToString(data)
            }
            HintType.EMPTY -> ""
            HintType.TEXT -> this.text ?: throw IllegalStateException("A hint of type  ${this.type.description} must have a valid text.")
            else -> throw IllegalStateException("The hint type ${this.type.description} is not supported.")
        }

        val contentType = when (this.type) {
            HintType.IMAGE -> ApiContentType.IMAGE
            HintType.VIDEO -> ApiContentType.VIDEO
            HintType.TEXT -> ApiContentType.TEXT
            HintType.EMPTY -> ApiContentType.EMPTY
            else ->  throw IllegalStateException("The hint type ${this.type.description} is not supported.")
        }

        return ApiContentElement(contentType = contentType, content = content, offset = this.start ?: 0L)
    }
}







