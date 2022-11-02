package dev.dres.data.model.run

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.CompetitionDescription
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

typealias EvaluationId = String

/**
 * Represents a [Evaluation], i.e., a concrete instance of a [CompetitionDescription], as executed by DRES.
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

    /** The [RunType] of this [Evaluation]. */
    var type by xdLink1(RunType)

    /** The [CompetitionDescription] backing this [Evaluation]. */
    var description by xdLink1(CompetitionDescription)

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
}