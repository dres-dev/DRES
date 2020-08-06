package dres.data.model.competition

import dres.api.rest.types.task.ContentElement
import dres.api.rest.types.task.TaskHint
import dres.api.rest.types.task.TaskTarget
import dres.data.model.Config
import dres.data.model.UID
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.validation.TemporalOverlapSubmissionValidator
import dres.run.validation.interfaces.SubmissionValidator
import dres.run.validation.judged.BasicJudgementValidator
import java.io.*
import kotlin.math.max

/**
 * Basic description of a [TaskDescription].
 *
 * @version 1.0.1
 * @author Luca Rossetto & Ralph Gasser
 */
class TaskDescription(

    /** Internal, unique ID of this [TaskDescription]. */
    val id: UID,

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

    /** List of [TaskDescriptionComponent]s that act as clues to find the target media. */
    val components: List<TaskDescriptionComponent>
){

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer = taskType.newScorer()

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionValidator].
     */
    fun newValidator(): SubmissionValidator = when(taskType.targetType){
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> TODO()
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionValidator(target as TaskDescriptionTarget.VideoSegmentTarget)
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TODO()
        TaskType.TargetType.JUDGEMENT -> BasicJudgementValidator()
    }

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
        val sequence = this.components.groupBy { it.contentType }.flatMap { group ->
            group.value.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toQueryContentElement(config))
                if (it.end != null) {
                    ret.add(ContentElement(contentType = ret.first().contentType, offset = it.end!!))
                }
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
    fun toTaskTarget(config: Config): TaskTarget? = this.target.toQueryContentElement(config)?.let { TaskTarget(this.id.string, listOf(it)) }

    /** Produces a Textual description of the content of the task if possible */
    fun textualDescription(): String = components.filterIsInstance(TaskDescriptionComponent.TextTaskDescriptionComponent::class.java)
            .maxBy { it.start ?: 0 }?.text ?: name

    /** Prints an overview of the task to a provided stream */
    fun printOverview(out: PrintStream) {
        out.println("$name: ${taskGroup.name} (${taskType.name})")
        out.println("Target: ${target.textDescription()}")
        out.println("Components: (${components.size})")
        components.sortedBy { it.start ?: 0}.forEach {
            out.println(it.textDescription())
        }
        out.println()
    }

    /**
     * Checks if no components of the same type overlap
     * @throws IllegalArgumentException
     */
    fun validate() {
        this.components.groupBy { it.contentType }.forEach { group ->
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