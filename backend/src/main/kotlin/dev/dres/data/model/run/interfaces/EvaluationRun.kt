package dev.dres.data.model.run.interfaces

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.DbEvaluation
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.toList

/**
 * Represents a [DbEvaluation] that a DRES user or client takes place in and that groups several [TaskRun]s
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface EvaluationRun: Run {
    /** The unique [EvaluationId] that identifies this [EvaluationRun]. */
    val id: EvaluationId

    /** The name human readable of this [EvaluationRun]. */
    val name: String

    /** Reference to the [DbEvaluationTemplate] that describes the content of this [EvaluationRun]. */
    val description: DbEvaluationTemplate

    /** Collection of [TaskRun]s that make up this [EvaluationRun]. */
    val tasks: List<TaskRun>

    /** Flag indicating that participants can also use the viewer for this [DbEvaluation]. */
    var participantCanView: Boolean

    /** Flag indicating that tasks can be repeated.*/
    var allowRepeatedTasks: Boolean

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews: Int

    /** List of [Scoreboard] implementations for this [EvaluationRun] */
    val scoreboards: List<Scoreboard>
}

