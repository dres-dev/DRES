package dev.dres.data.model.run.interfaces

import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.run.DbEvaluation
import dev.dres.run.score.scoreboard.Scoreboard

/**
 * Represents a [DbEvaluation] that a DRES user or client takes place in and that groups several [TaskRun]s
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface EvaluationRun: Run {
    /** The unique [EvaluationId] that identifies this [EvaluationRun]. */
    val id: EvaluationId

    /** The name human-readable of this [EvaluationRun]. */
    val name: String

    /** Reference to the [ApiEvaluationTemplate] that describes the content of this [EvaluationRun]. */
    val template: ApiEvaluationTemplate

    /** Collection of [TaskRun]s that make up this [EvaluationRun]. */
    val tasks: List<TaskRun>

    /** Flag indicating that participants can also use the viewer for this [EvaluationRun]. */
    val participantCanView: Boolean

    /** Flag indicating that tasks can be repeated.*/
    val allowRepeatedTasks: Boolean

    /** A fixed limit on submission previews. */
    val limitSubmissionPreviews: Int

    /** List of [Scoreboard] implementations for this [EvaluationRun] */
    val scoreboards: List<Scoreboard>
}

