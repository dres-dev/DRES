package dev.dres.data.model.run.interfaces

import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.Competition
import dev.dres.data.model.run.CompetitionId
/**
 * Represents a [Competition] that a DRES user or client takes place in and that groups several [Task]s
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Competition: Run {
    /** The unique [CompetitionId] that identifies this [Competition]. */
    val id: CompetitionId

    /** The name human readable of this [Competition]. */
    val name: String

    /** Reference to the [CompetitionDescription] that describes the content of this [Competition]. */
    val description: CompetitionDescription

    /** Collection of [Task]s that make up this [Competition]. */
    val tasks: List<Task>

    /** Flag indicating that participants can also use the viewer for this [Competition]. */
    var participantCanView: Boolean

    /** Flag indicating that tasks can be repeated.*/
    var allowRepeatedTasks: Boolean

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews: Int
}