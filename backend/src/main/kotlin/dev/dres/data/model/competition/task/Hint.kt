package dev.dres.data.model.competition.task

import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
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
 * Represents the hint given by a [TaskDescription], e.g., a media item or text that is shown.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class Hint(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<Hint>()

    /** The [HintType] of this [Hint]. */
    var type by xdLink1(HintType)

    /** The start of a (potential) range. */
    var start by xdNullableLongProp() { min(0L) }

    /** The start of a (potential) range. */
    var end by xdNullableLongProp { min(this@Hint.start ?: 0L) }

    /** The parent [TaskDescription] this [Hint] belongs to. */
    var task by xdParent<Hint,TaskDescription>(TaskDescription::hints)

    /** The[MediaItem] shown as part of the [Hint]. Can be null. */
    var hintItem by xdLink0_1(MediaItem)

    /** The target text. Can be null. */
    var hintText by xdStringProp() { requireIf { type == HintType.TEXT }}

    /** The target text. Can be null. */
    var hintExternalLocation by xdStringProp()

    /** The start of a (potential) range. */
    var temporalRangeStart by xdNullableIntProp { requireIf { type == HintType.VIDEO } }

    /** The start of a (potential) range. */
    var temporalRangeEnd by xdNullableIntProp { requireIf { type == HintType.VIDEO  } }

    /** Returns the [TemporalRange] of this [TaskDescriptionTarget]. */
    val range: TemporalRange?
        get() {
            return if (this.temporalRangeStart != null && this.temporalRangeEnd != null) {
                return TemporalRange(TemporalPoint.Frame(this.temporalRangeStart!!, this.hintItem?.fps ?: 1.0f), TemporalPoint.Frame(this.temporalRangeEnd!!, this.hintItem?.fps ?: 1.0f))
            } else {
                null
            }
        }

    /**
     * Generates and returns a textual description of this [Hint].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        HintType.TEXT -> "\"${this.hintText}\" from ${this.start ?: "beginning"} to ${end ?: "end"}"
        HintType.VIDEO -> {
            if (this.hintItem != null) {
                "Image ${this.hintItem!!.name} from ${start ?: "beginning"} to ${end ?: "end"}"
            } else {
                "Image ${this.hintExternalLocation} from ${start ?: "beginning"} to ${end ?: "end"}"
            }
        }
        HintType.IMAGE -> {
            if (this.hintItem != null) {
                "Image ${this.hintItem!!.name}"
            } else {
                "Image ${this.hintExternalLocation}"
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
                val path = if (this.hintItem != null) {
                    this.hintItem!!.pathToCachedItem(config, this.temporalRangeStart, this.temporalRangeEnd)
                } else if (this.hintExternalLocation != null) {
                    Paths.get(this.hintExternalLocation!!)
                } else {
                    throw IllegalArgumentException("A hint of type  ${this.type.description} must have a valid media item or external path.")
                }
                val data = Files.newInputStream(path, StandardOpenOption.READ).use { stream ->
                    stream.readAllBytes()
                }
                Base64.getEncoder().encodeToString(data)
            }
            HintType.EMPTY -> ""
            HintType.TEXT -> this.hintText ?: throw IllegalStateException("A hint of type  ${this.type.description} must have a valid text.")
            else -> throw IllegalStateException("The content type ${this.type.description} is not supported.")
        }
        return ApiContentElement(contentType = this.type.toApi(), content = content, offset = this.start ?: 0L)
    }
}







