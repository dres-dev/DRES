package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiEvaluation
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

typealias EvaluationId = String

/**
 * Represents a [DbEvaluation], i.e., a concrete instance of a [DbEvaluationTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbEvaluation(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbEvaluation>()

    /** The [EvaluationId] of this [DbEvaluation]. */
    var evaluationId: EvaluationId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbEvaluation]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbEvaluationType] of this [DbEvaluation]. */
    var type by xdLink1(DbEvaluationType)

    /** The [DbEvaluationTemplate] backing this [DbEvaluation]. */
    var template by xdLink1(DbEvaluationTemplate)

    /** Timestamp of when this [DbEvaluation] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [DbEvaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [DbTask]s that belong to this [DbEvaluation]. */
    val tasks by xdChildren0_N<DbEvaluation,DbTask>(DbTask::evaluation)

    /** Flag indicating that participants can also use the viewer for this [DbEvaluation]. */
    var participantCanView by xdBooleanProp()

    /** Flag indicating that tasks should be shuffled. is only used for asynchronous runs */
    var shuffleTasks by xdBooleanProp()

    /** Flag indicating that tasks can be repeated. is only used for asynchronous runs */
    var allowRepeatedTasks by xdBooleanProp()

    /** A fixed limit on submission previews. */
    var limitSubmissionPreviews by xdIntProp()

    /**
     * Converts this [DbEvaluation] to a RESTful API representation [ApiEvaluation].
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
     * Generates and returns an [EvaluationRun] instance for this [DbEvaluation].
     *
     * @return [EvaluationRun]
     */
    fun toRun(): EvaluationRun = when(this.type) {
        DbEvaluationType.INTERACTIVE_SYNCHRONOUS -> InteractiveSynchronousEvaluation(this)
        DbEvaluationType.INTERACTIVE_ASYNCHRONOUS -> InteractiveAsynchronousEvaluation(this, emptyMap()) /* TODO: Not sure about semantics here. */
        DbEvaluationType.NON_INTERACTIVE -> NonInteractiveEvaluation(this)
        else -> throw IllegalArgumentException("Unsupported run type ${this.type.description}. This is a programmer's error!")
    }
}