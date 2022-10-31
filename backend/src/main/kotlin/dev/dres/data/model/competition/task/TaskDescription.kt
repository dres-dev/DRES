package dev.dres.data.model.competition.task

import dev.dres.api.rest.types.competition.tasks.*
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.competition.interfaces.SubmissionFilterFactory
import dev.dres.data.model.competition.interfaces.TaskScorerFactory
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.run.interfaces.Task
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.simple.min
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Long.max

typealias TaskDescriptionId = String

/**
 * Basic description of a [Task] as executed in DRES. Defines basic attributes such as its name, its duration,
 * the [TaskDescriptionTarget] and the [Hint]s, that should be presented to the user.
 *
 * @version 2.0.0
 * @author Luca Rossetto & Ralph Gasser
 */
class TaskDescription(entity: Entity) : PersistentEntity(entity), TaskScorerFactory, SubmissionFilterFactory {
    companion object: XdNaturalEntityType<TaskDescription>()

    /** The [TaskDescriptionId] of this [TaskDescription]. */
    var taskId: TaskDescriptionId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [Team]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [TaskGroup] this [TaskDescription] belongs to. */
    var taskGroup by xdParent<TaskDescription, TaskGroup>(TaskGroup::tasks)

    /** The [MediaCollection] this [TaskDescription] operates upon. */
    var collection by xdLink1(MediaCollection)

    /** The duration of the [TaskDescription] in seconds. */
    var duration by xdRequiredLongProp { min(0L)  }

    /** The [TaskDescriptionTarget]s that identify the target. Multiple entries indicate the existence of multiple targets. */
    val targets by xdChildren1_N<TaskDescription, TaskDescriptionTarget>(TaskDescriptionTarget::task)

    /** The [Hint]s that act as clues to find the target media. */
    val hints by xdChildren0_N<TaskDescription,Hint>(Hint::task)

    /**
     * Generates a new [TaskScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskScorer].
     */
    override fun newScorer(): TaskScorer = this.taskGroup.type.newScorer()


    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    override fun newFilter(): SubmissionFilter = this.taskGroup.type.newFilter()


    /**
     * Generates and returns a [ApiHintContent] object to be used by the RESTful interface.
     *
     * Requires a valid transaction.
     *
     * @param config The [Config] used of path resolution.
     * @return [ApiHintContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskHint(config: Config): ApiHintContent {
        val sequence =  this.hints.asSequence().groupBy { it.type }.flatMap { group ->
            var index = 0
            group.value.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toQueryContentElement(config))
                if (it.end != null) {
                    if (index == (group.value.size - 1)) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    } else if ((group.value[index+1].start ?: 0) > it.end!!) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    }
                }
                index += 1
                ret
            }
        }
        return ApiHintContent(this.id, sequence, false)
    }

    /**
     * Generates and returns a [ApiTargetContent] object to be used by the RESTful interface.
     *
     * Requires a valid transaction.
     *
     * @param config The [Config] used of path resolution.
     * @return [ApiTargetContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskTarget(config: Config): ApiTargetContent {
        var cummulativeOffset = 0L
        val sequence = this.targets.asSequence().flatMap {
            cummulativeOffset += Math.floorDiv(it.item!!.durationMs!!, 1000L) + 1L
            listOf(
                it.toQueryContentElement(config),
                ApiContentElement(ApiContentType.EMPTY, null, cummulativeOffset)
            )
        }.toList()
        return ApiTargetContent(this.id, sequence)
    }

    /**
     * Produces a Textual description of the content of the [TaskDescription] if possible
     *
     * @return Textual description of this [TaskDescription]'s content,
     */
    fun textualDescription(): String = this.hints.asSequence().filter { it.type == HintType.TEXT }.maxByOrNull { it.start ?: 0 }?.text ?: name

    /**
     * Converts this [TaskDescription] to a RESTful API representation [ApiTaskDescription].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeam]
     */
    fun toApi(): ApiTaskDescription = ApiTaskDescription(
        this.id,
        this.name,
        this.taskGroup.name,
        this.taskGroup.type.name,
        this.duration,
        this.collection.id,
        this.targets.asSequence().map { it.toApi() }.toList(),
        this.hints.asSequence().map { it.toApi() }.toList()
    )

    /**
     * Checks if no components of the same type overlap
     *
     * @throws IllegalArgumentException
     */

    fun validate() {
        this.hints.asSequence().groupBy { it.type }.forEach { group ->
            var end = 0L
            group.value.sortedBy { it.start ?: 0 }.forEach {
                if((it.start ?: end) < end){
                    throw IllegalArgumentException("Overlapping component of type ${group.key} in task $name")
                }
                end = max(end, it.end ?: ((it.start ?: 0L) + 1L))
            }
        }
    }
}