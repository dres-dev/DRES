package dev.dres.data.model.competition.task

import dev.dres.api.rest.types.competition.tasks.ApiTarget
import dev.dres.api.rest.types.competition.tasks.ApiTargetItem
import dev.dres.api.rest.types.competition.tasks.RestTemporalRange
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.team.Team
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
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Represents the target of a [TaskDescription], i.e., the [MediaItem] or part thereof that is considered correct.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskDescriptionTarget(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<TaskDescriptionTarget>()

     /** The [TargetType] of this [TaskDescriptionTarget]. */
    var type by xdLink1(TargetType)

    /** The parent [TaskDescription] this [TaskDescriptionTarget] belongs to. */
    var task by xdParent<TaskDescriptionTarget,TaskDescription>(TaskDescription::targets)

    /** The targeted  [MediaItem]. Can be null. */
    var item by xdLink0_1(MediaItem)

    /** The target text. Can be null. */
    var text by xdStringProp() { requireIf { item == null }}

    /** The start of a (potential) range. */
    var temporalRangeStart by xdNullableIntProp { requireIf { item?.type == MediaType.VIDEO } }

    /** The start of a (potential) range. */
    var temporalRangeEnd by xdNullableIntProp { requireIf { item?.type == MediaType.VIDEO } }

    /** Returns the [TemporalRange] of this [TaskDescriptionTarget]. */
    val range: TemporalRange?
        get() {
            return if (this.temporalRangeStart != null && this.temporalRangeEnd != null) {
                return TemporalRange(TemporalPoint.Frame(this.temporalRangeStart!!, this.item!!.fps ?: 1.0f), TemporalPoint.Frame(this.temporalRangeEnd!!, this.item!!.fps ?: 1.0f))
            } else {
                null
            }
        }

    /**
     * Generates and returns a textual description of this [TaskDescriptionTarget].
     *
     * @return Text
     */
    fun textDescription(): String = when (this.type) {
        TargetType.JUDGEMENT -> "Judgement"
        TargetType.JUDGEMENT_WITH_VOTE -> "Judgement with vote"
        TargetType.MEDIA_ITEM -> "Media item ${this.item?.name}"
        TargetType.MEDIA_ITEM_TEMPORAL_RANGE -> "Media item ${this.item?.name} @ ${this.temporalRangeStart} - ${this.temporalRangeEnd}"
        TargetType.TEXT -> "Text: ${this.text}"
        else -> throw IllegalStateException("The task description type ${this.type.description} is currently not supported.")
    }

    /**
     *
     */
    fun toApi() = if (this.temporalRangeStart != null && this.temporalRangeEnd != null) {
        ApiTarget(this.type.toApi(), ApiTargetItem(this.item.id))
    } else {
        ApiTarget(this.type.toApi(), ApiTargetItem(this.item.id))
    }
        /**
         * Generates a [ApiTarget] from a [TaskDescriptionTarget] and returns it.
         *
         * @param target The [TaskDescriptionTarget] to convert.
         */
        fun fromTarget(target: TaskDescriptionTarget) = when(target.type) {
            is TaskDescriptionTarget.JudgementTaskDescriptionTarget -> ApiTarget(TargetOption.JUDGEMENT, target.targets.map { ApiTargetItem(it.first.id.string, if (it.second == null) null else RestTemporalRange(it.second!!)) })
            is TaskDescriptionTarget.VideoSegmentTarget -> ApiTarget(TargetOption.SINGLE_MEDIA_SEGMENT, listOf(
                ApiTargetItem(target.item.id.string, RestTemporalRange(target.temporalRange))
            ))
            is TaskDescriptionTarget.MediaItemTarget -> ApiTarget(TargetOption.SINGLE_MEDIA_ITEM, listOf(ApiTargetItem(target.item.id.string)))
            is TaskDescriptionTarget.MultipleMediaItemTarget -> ApiTarget(TargetOption.MULTIPLE_MEDIA_ITEMS, target.items.map { ApiTargetItem(it.id.string) })
            is TaskDescriptionTarget.VoteTaskDescriptionTarget -> ApiTarget(TargetOption.VOTE, target.targets.map { ApiTargetItem(it.first.id.string, if (it.second == null) null else RestTemporalRange(it.second!!)) })
            is TaskDescriptionTarget.TextTaskDescriptionTarget -> ApiTarget(TargetOption.TEXT, target.targets.map { ApiTargetItem(it, null) })
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
            TargetType.JUDGEMENT,
            TargetType.JUDGEMENT_WITH_VOTE -> null
            TargetType.MEDIA_ITEM -> {
                val path = this.item?.pathToCachedItem(config, this.temporalRangeStart, this.temporalRangeEnd)
                    ?:  throw IllegalArgumentException("A target of type  ${this.type.description} must have a valid media item.")
                val data = Files.newInputStream(path, StandardOpenOption.READ).use { stream ->
                    stream.readAllBytes()
                }
                Base64.getEncoder().encodeToString(data)
            }
            TargetType.TEXT -> this.text
            else -> throw IllegalStateException("The content type ${this.type.description} is not supported.")
        }
        return ApiContentElement(contentType = this.type.toApi(), content = content, offset = 0L)
    }
}