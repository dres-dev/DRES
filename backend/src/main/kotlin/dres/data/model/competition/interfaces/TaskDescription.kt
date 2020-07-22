package dres.data.model.competition.interfaces

import dres.data.model.Config
import dres.data.model.competition.*
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.validation.interfaces.SubmissionValidator
import java.io.*
import java.util.*

/**
 * Basic description of a [Task].
 */

class TaskDescription(

    /** Internal, unique ID of this [TaskDescription]. */
    val uid: String,

    /** The name of the task */
    val name: String,

    /** The [TaskGroup]  the [Task] belongs to */
    val taskGroup: TaskGroup,

    /** The duration of the [TaskDescription] in seconds. */
    val duration: Long,

    /** The id of the relevant media collection for this task, if not otherwise specified */
    val defaultMediaCollectionId: Long,

    /** */
    val components: List<TaskDescriptionComponent>,

    /** */
    val target: TaskDescriptionTarget
){

    /**
     * Generates a new [TaskRunScorer] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [TaskRunScorer].
     */
    fun newScorer(): TaskRunScorer = taskGroup.type.newScorer()

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionValidator].
     */
    fun newValidator(): SubmissionValidator {
        TODO()
    }

    /**
     * Generates and returns a [SubmissionValidator] instance for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter = taskGroup.type.newFilter()

    /**
     * Generates a [QueryDescription] object to be used by a viewer
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toQueryDescription(config: Config): QueryDescription = QueryDescription(
            name,
            query = componentsToQueryContent(config),
            reveal = targetToQueryContent(config)
    )

    private fun componentsToQueryContent(config: Config): QueryContent {
        TODO()
    }

    private fun targetToQueryContent(config: Config): QueryContent = when (target) {
        is MediaSegmentTarget -> {
            val file = File(File(config.cachePath + "/tasks"), target.cacheItemName())
            FileInputStream(file).use { imageInFile ->
                val fileData = ByteArray(file.length().toInt())
                imageInFile.read(fileData)
                QueryContent(video = listOf(QueryContentElement(Base64.getEncoder().encodeToString(fileData), "video/mp4")))
            }
        }
        else -> throw IllegalStateException("transformation from ${target::javaClass} to QueryContent not implemented")
    }

    /** Prints an overview of the task to a provided stream */
    fun printOverview(out: PrintStream) {
        TODO()
    }

    /** Produces a Textual description of the content of the task if possible */
    fun textualDescription(): String {
        TODO()
    }
}