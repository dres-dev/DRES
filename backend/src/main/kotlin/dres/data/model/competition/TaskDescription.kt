package dres.data.model.competition

import dres.api.rest.types.query.ContentType
import dres.api.rest.types.query.QueryContentElement
import dres.api.rest.types.query.QueryHint
import dres.api.rest.types.query.QueryTarget
import dres.data.model.Config
import dres.data.model.UID
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.validation.TemporalOverlapSubmissionValidator
import dres.run.validation.interfaces.SubmissionValidator
import dres.run.validation.judged.BasicJudgementValidator
import java.io.*
import java.util.*
import kotlin.math.max

/**
 * Basic description of a [TaskDescription].
 *
 * @version 1.0
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
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionValidator(target as TaskDescriptionTarget.MediaSegmentTarget)
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
     * Generates and returns a [QueryHint] object to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [QueryHint]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toQueryDescription(config: Config): QueryHint {
        val sequence = this.components.map {
            when(it) {
                is TaskDescriptionComponent.TextTaskDescriptionComponent -> QueryContentElement(it.text, ContentType.TEXT)
                is TaskDescriptionComponent.ImageItemTaskDescriptionComponent -> {
                    val file = File(config.cachePath + "/tasks")
                    FileInputStream(file).use { imageInFile ->
                        val fileData = ByteArray(file.length().toInt())
                        imageInFile.read(fileData)
                        QueryContentElement(Base64.getEncoder().encodeToString(fileData), ContentType.IMAGE)
                    }
                }
                is TaskDescriptionComponent.VideoItemSegmentTaskDescriptionComponent -> {
                    val file = File(config.cachePath + "/tasks", it.cacheItemName())
                    FileInputStream(file).let { imageInFile ->
                        val fileData = ByteArray(file.length().toInt())
                        imageInFile.read(fileData)
                        QueryContentElement(Base64.getEncoder().encodeToString(fileData), ContentType.VIDEO)
                    }
                }
                else -> throw IllegalStateException("Transformation from ${target::javaClass} to query hint currently not implemented.")
            }
        }
        return QueryHint(this.id.string, sequence, false)
    }

    /**
     * Generates and returns a [QueryTarget] object to be used by the RESTful interface.
     *
     * @param config The [Config] used of path resolution.
     * @return [QueryTarget]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private fun toQueryTarget(config: Config): QueryTarget = when (this.target) {
        is TaskDescriptionTarget.MediaSegmentTarget -> {
            val file = File(File(config.cachePath + "/tasks"), target.cacheItemName())
            FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                QueryTarget(this.id.string, listOf(QueryContentElement(Base64.getEncoder().encodeToString(fileData), ContentType.VIDEO)))
            }
        }
        else -> throw IllegalStateException("Transformation from ${target::javaClass} to query harget currently not implemented.")
    }

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

    /** Produces a Textual description of the content of the task if possible */
    fun textualDescription(): String = components.filterIsInstance(TaskDescriptionComponent.TextTaskDescriptionComponent::class.java)
            .maxBy { it.start ?: 0 }?.text ?: name

    /**
     * Checks if no components of the same type overlap
     * @throws IllegalArgumentException
     */
    fun validate() {
        this.components.groupBy { it.contentType }.forEach {group ->
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