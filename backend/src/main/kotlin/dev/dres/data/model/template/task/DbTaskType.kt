package dev.dres.data.model.template.task

import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.api.rest.types.template.tasks.TaskTypeId
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.task.options.DbConfiguredOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.*
import kotlinx.dnq.simple.min

/**
 * Specifies the type of a [DbTaskTemplate] and allows for many aspects of its configuration.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class DbTaskType(entity: Entity) : PersistentEntity(entity) {
    /** Combination of [DbTaskType] name / competition must be unique. */
    companion object: XdNaturalEntityType<DbTaskType>() {
        override val compositeIndices = listOf(
            listOf(DbTaskType::name, DbTaskType::evaluation)
        )
    }

    /** The [TaskTypeId] of this [DbTaskType]. */
    var taskTypeId: TaskTypeId
        get() = this.id
        set(value) { this.id = value }

    /** The name of this [DbTaskType]. */
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [DbEvaluationTemplate] this [DbTaskType] belongs to. */
    var evaluation: DbEvaluationTemplate by xdParent<DbTaskType,DbEvaluationTemplate>(DbEvaluationTemplate::taskTypes)

    /** The (default) duration of this [DbTaskType] in seconds. */
    var duration by xdRequiredLongProp() { min(0L) }

    /** The [DbTargetOption] for this [DbTaskType]. Specifies the type of target. */
    var target by xdLink1(DbTargetOption)

    /** The [DbHintOption]s that make-up this [DbTaskType]. */
    val hints by xdLink0_N(DbHintOption)

    /** The [DbSubmissionOption]s for this [DbTaskType]. */
    val submission by xdLink0_N(DbSubmissionOption)

    /** The [DbScoreOption] for this [DbTaskType]. Specifies the type of scorer that should be used. */
    var score by xdLink1(DbScoreOption)

    /** The [DbTaskOption]s for this [DbTaskType]. */
    val options by xdLink0_N(DbTaskOption)

    /** [DbConfiguredOption]s registered for this [DbTaskTemplate]. */
    val configurations by xdChildren0_N<DbTaskType,DbConfiguredOption>(DbConfiguredOption::task)

    /**
     * Converts this [DbTaskType] to a RESTful API representation [ApiTaskType].
     *
     * @return [ApiTaskType]
     */
    fun toApi(): ApiTaskType = ApiTaskType(
        id=this.xdId,
        name = this.name,
        duration = this.duration,
        targetOption = this.target.toApi(),
        hintOptions = this.hints.asSequence().map { it.toApi() }.toList(),
        submissionOptions = this.submission.asSequence().map { it.toApi() }.toList(),
        taskOptions = this.options.asSequence().map { it.toApi() }.toList(),
        scoreOption = this.score.toApi(),
        configuration = this.configurations.asSequence().map { it.key to it.value }.toMap()
    )
}
