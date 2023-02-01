package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiEvaluation
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

typealias TaskId = String

/**
 * Represents a [Evaluation], i.e., a concrete instance of a [EvaluationTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Evaluation(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<Evaluation>()

    /** The [EvaluationId] of this [Evaluation]. */
    var evaluationId: EvaluationId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [Evaluation]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [EvaluationType] of this [Evaluation]. */
    var type by xdLink1(EvaluationType)

    /** The [EvaluationTemplate] backing this [Evaluation]. */
    var template by xdLink1(EvaluationTemplate)

    /** Timestamp of when this [Evaluation] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [Evaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [Task]s that belong to this [Evaluation]. */
    val tasks by xdChildren0_N<Evaluation,Task>(Task::evaluation)

    /** Flag indicating that participants can also use the viewer for this [Evaluation]. */
    var participantCanView by xdBooleanProp()

    /** Flag indicating that tasks should be shuffled. is only used for asynchronous runs */
    var shuffleTasks by xdBooleanProp()

    /** Flag indicating that tasks can be repeated. is only used for asynchronous runs */
    var allowRepeatedTasks by xdBooleanProp()

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews by xdIntProp()

    /**
     * Converts this [Evaluation] to a RESTful API representation [ApiEvaluation].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiEvaluation]
     */
    fun toApi(): ApiEvaluation = ApiEvaluation(
        evaluationId = this.evaluationId,
        name = this.name,
        type = this.type.toApi(),
        template = this.template.toApi(),
        started = this.started,
        ended = this.ended,
        tasks = this.tasks.asSequence().map { it.toApi() }.toList()
    )

    /**
     * Generates and returns an [EvaluationRun] instance for this [Evaluation].
     *
     * @return [EvaluationRun]
     */
    fun toRun(): EvaluationRun = when(this.type) {
        EvaluationType.INTERACTIVE_SYNCHRONOUS -> InteractiveSynchronousEvaluation(this)
        EvaluationType.INTERACTIVE_ASYNCHRONOUS -> InteractiveAsynchronousEvaluation(this, emptyMap()) /* TODO: Not sure about semantics here. */
        EvaluationType.NON_INTERACTIVE -> NonInteractiveEvaluation(this)
        else -> throw IllegalArgumentException("Unsupported run type ${this.type.description}. This is a programmer's error!")
    }
}