package dev.dres.data.model.template.task

import dev.dres.api.rest.types.competition.tasks.ApiTargetType
import dev.dres.api.rest.types.competition.tasks.ApiTaskGroup
import dev.dres.data.model.template.EvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A [TaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskGroup(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<TaskGroup>() {
        /** Combination of [TaskGroup] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(TaskGroup::name, TaskGroup::competition)
        )
    }

    /** The name of this [TaskGroup]. */
    var name: String by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [TaskType] this [TaskGroup] belongs to.*/
    var type by xdLink1(TaskType)

    /** The [EvaluationTemplate] this [TaskGroup] belongs to. */
    var competition by xdParent<TaskGroup, EvaluationTemplate>(EvaluationTemplate::taskGroups)

    /**
     * Converts this [TargetType] to a RESTful API representation [ApiTargetType].
     *
     * This is a convenience method and it requires and active transaction context.
     *
     * @return [ApiTargetType]
     */
    fun toApi(): ApiTaskGroup
        = ApiTaskGroup(this.name,this.type.name)
}