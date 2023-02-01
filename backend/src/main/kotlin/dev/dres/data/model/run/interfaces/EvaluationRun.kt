package dev.dres.data.model.run.interfaces

import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.run.Evaluation
import dev.dres.data.model.run.EvaluationId
/**
 * Represents a [Evaluation] that a DRES user or client takes place in and that groups several [TaskRun]s
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface EvaluationRun: Run {
    /** The unique [EvaluationId] that identifies this [EvaluationRun]. */
    val id: EvaluationId

    /** The name human readable of this [EvaluationRun]. */
    val name: String

    /** Reference to the [EvaluationTemplate] that describes the content of this [EvaluationRun]. */
    val description: EvaluationTemplate

    /** Collection of [TaskRun]s that make up this [EvaluationRun]. */
    val tasks: List<TaskRun>

    /** Flag indicating that participants can also use the viewer for this [Evaluation]. */
    var participantCanView: Boolean

    /** Flag indicating that tasks can be repeated.*/
    var allowRepeatedTasks: Boolean

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews: Int
}