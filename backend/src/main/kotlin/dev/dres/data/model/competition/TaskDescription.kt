package dev.dres.data.model.competition

import dev.dres.api.rest.types.task.ContentElement
import dev.dres.api.rest.types.task.TaskHint
import dev.dres.api.rest.types.task.TaskTarget
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintStream
import kotlin.math.max

typealias TaskDescriptionId = UID

/**
 * Basic description of a [TaskDescription].
 *
 * @version 1.0.1
 * @author Luca Rossetto & Ralph Gasser
 */
class TaskDescription(

    /** Internal, unique ID of this [TaskDescription]. */
    val id: TaskDescriptionId,

    /** The name of the task */
    val name: String,

    /** The [TaskGroup] this [TaskDescription] belongs to. */
    val taskGroup: TaskGroup,

    /** The [TaskType] this [TaskDescription] belongs to. */
    val taskType: TaskType,

    /** The duration of the [TaskDescription] in seconds. */
    val duration: Long,

    /** The id of the relevant media collection for this task, if not otherwise specified */
    val mediaCollectionId: UID,

    /** The [TaskDescriptionTarget] that identifies the target media. */
    val target: TaskDescriptionTarget,

    /** List of [TaskDescriptionHint]s that act as clues to find the target media. */
    val hints: List<TaskDescriptionHint>
){

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer = taskType.newScorer()


    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter = taskType.newFilter()

    /**
     * Generates and returns a [TaskHint] object to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [TaskHint]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskHint(config: Config): TaskHint {
        val sequence = this.hints.groupBy { it.contentType }.flatMap { group ->
            var index = 0
            group.value.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toQueryContentElement(config))
                if (it.end != null) {
                    if (index == (group.value.size - 1)) {
                        ret.add(ContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    } else if ((group.value[index+1].start ?: 0) > it.end!!) {
                        ret.add(ContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    }
                }
                index += 1
                ret
            }
        }
        return TaskHint(this.id.string, sequence, false)
    }

    /**
     * Generates and returns a [TaskTarget] object to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [TaskTarget]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskTarget(config: Config, collections: DAO<MediaCollection>): TaskTarget = this.target.toQueryContentElement(config, collections).let { TaskTarget(this.id.string, it) }

    /** Produces a Textual description of the content of the task if possible */
    fun textualDescription(): String = hints.filterIsInstance(TaskDescriptionHint.TextTaskDescriptionHint::class.java)
            .maxByOrNull { it.start ?: 0 }?.text ?: name

    /** Prints an overview of the task to a provided stream */
    fun printOverview(out: PrintStream) {
        out.println("$name: ${taskGroup.name} (${taskType.name})")
        out.println("Target: ${target.textDescription()}")
        out.println("Components: (${hints.size})")
        hints.sortedBy { it.start ?: 0}.forEach {
            out.println(it.textDescription())
        }
        out.println()
    }

    /**
     * Checks if no components of the same type overlap
     * @throws IllegalArgumentException
     */
    fun validate() {
        this.hints.groupBy { it.contentType }.forEach { group ->
            var end = 0L
            group.value.sortedBy { it.start ?: 0 }.forEach {
                if((it.start ?: end) < end){
                    throw IllegalArgumentException("Overlapping component of type ${group.key} in task $name")
                }
                end = max(end, it.end ?: (it.start ?: 0) + 1)
            }
        }
    }
}