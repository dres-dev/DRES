package dev.dres.data.model.template.task

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.api.rest.types.competition.tasks.ApiTarget
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.Config
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.media.time.TemporalPoint
import dev.dres.data.model.media.time.TemporalRange
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.simple.requireIf
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Represents the target of a [TaskTemplate], i.e., the [MediaItem] or part thereof that is considered correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskTemplateTarget(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<TaskTemplateTarget>()

     /** The [TargetType] of this [TaskTemplateTarget]. */
    var type by xdLink1(TargetType)

    /** The parent [TaskTemplate] this [TaskTemplateTarget] belongs to. */
    var task by xdParent<TaskTemplateTarget,TaskTemplate>(TaskTemplate::targets)

    /** The targeted  [MediaItem]. Can be null. */
    var item by xdLink0_1(MediaItem)

    /** The target text. Can be null. */
    var text by xdStringProp() { requireIf { item == null }}

    /** The start of a (potential) range. */
    var start by xdNullableLongProp { requireIf { item?.type == MediaType.VIDEO } }

    /** The start of a (potential) range. */
    var end by xdNullableLongProp { requireIf { item?.type == MediaType.VIDEO } }

    /** Returns the [TemporalRange] of this [TaskTemplateTarget]. */
    val range: TemporalRange?
        get() = if (this.start != null && this.end != null) {
            TemporalRange(TemporalPoint.Millisecond(this.start!!), TemporalPoint.Millisecond(this.end!!))
        } else {
            null
        }


    /**
     * Generates and returns a textual description of this [TaskTemplateTarget].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        TargetType.JUDGEMENT -> "Judgement"
        TargetType.JUDGEMENT_WITH_VOTE -> "Judgement with vote"
        TargetType.MEDIA_ITEM -> "Media item ${this.item?.name}"
        TargetType.MEDIA_ITEM_TEMPORAL_RANGE -> "Media item ${this.item?.name} @ ${this.start} - ${this.end}"
        TargetType.TEXT -> "Text: ${this.text}"
        else -> throw IllegalStateException("The task description type ${this.type.description} is currently not supported.")
    }

    /**
     *
     */
    fun toApi(): ApiTarget = when(this.type) {
        TargetType.JUDGEMENT,
        TargetType.JUDGEMENT_WITH_VOTE -> ApiTarget(this.type.toApi(), null)
        TargetType.MEDIA_ITEM,
        TargetType.MEDIA_ITEM_TEMPORAL_RANGE -> ApiTarget(this.type.toApi(), this.item?.id, this.range?.let { ApiTemporalRange(it) })
        TargetType.TEXT -> ApiTarget(this.type.toApi(), this.text)
        else -> throw IllegalStateException("Task description of type ${this.type.description} is not supported.")
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
        val (content, type) = when (this.type) {
            TargetType.JUDGEMENT,
            TargetType.JUDGEMENT_WITH_VOTE -> null to ApiContentType.EMPTY
            TargetType.MEDIA_ITEM -> {
                val type = when (this.item?.type) {
                    MediaType.VIDEO -> ApiContentType.VIDEO
                    MediaType.IMAGE -> ApiContentType.IMAGE
                    else -> throw IllegalStateException("Invalid target description; type indicates presence of media item but item seems unsupported or unspecified.")
                }
                val path = Paths.get(config.cachePath, this.item?.cachedItemName(this.start, this.end))
                    ?:  throw IllegalArgumentException("A target of type  ${this.type.description} must have a valid media item.")
                val data = Files.newInputStream(path, StandardOpenOption.READ).use { stream ->
                    stream.readAllBytes()
                }
                Base64.getEncoder().encodeToString(data) to type
            }
            TargetType.TEXT -> this.text to ApiContentType.TEXT
            else -> throw IllegalStateException("The content type ${this.type.description} is not supported.")
        }
        return ApiContentElement(contentType = type, content = content, offset = 0L)
    }
}