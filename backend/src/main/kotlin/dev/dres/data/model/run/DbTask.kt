package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiTask
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.template.team.DbTeam
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

typealias EvaluationId = String

/**
 * Represents a [DbTask], i.e., a concrete instance of a [DbTaskTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTask(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbTask>()

    /** The [EvaluationId] of this [DbTask]. */
    var taskId: EvaluationId
        get() = this.id
        set(value) { this.id = value }

    /** Timestamp of when this [DbEvaluation] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [DbEvaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [DbTaskTemplate] this [DbTask] is an instance of. */
    var template by xdLink1(DbTaskTemplate)

    /** Link to a [DbTeam] this [DbTask] was created for. Can be NULL!*/
    var team by xdLink0_1(DbTeam)

    /** The [DbEvaluation] this [DbTask] belongs to. */
    var evaluation: DbEvaluation by xdParent<DbTask,DbEvaluation>(DbEvaluation::tasks)

    /** List of [DbSubmission]s received by this [DbTask]. */
    val submissions by xdChildren0_N<DbTask,DbAnswerSet>(DbAnswerSet::task)

    /**
     * Converts this [DbTask] to a RESTful API representation [ApiTask].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTask]
     */
    fun toApi(): ApiTask = ApiTask(
        taskId = this.taskId,
        templateId = this.template.id,
        started = this.started,
        ended = this.ended,
        submissions = this.submissions.asSequence().map { it.toApi() }.toList()
    )
}