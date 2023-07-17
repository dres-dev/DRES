package dev.dres.data.model.template.task

import dev.dres.api.rest.types.template.tasks.ApiTargetType
import dev.dres.api.rest.types.template.tasks.ApiTaskGroup
import dev.dres.data.model.template.DbEvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy

/**
 * A [DbTaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class DbTaskGroup(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbTaskGroup>() {
        /** Combination of [DbTaskGroup] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(DbTaskGroup::name, DbTaskGroup::evaluation)
        )
    }

    /** The name of this [DbTaskGroup]. */
    var name: String by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [DbTaskType] this [DbTaskGroup] belongs to.*/
    var type by xdLink1(DbTaskType, onTargetDelete = OnDeletePolicy.CASCADE)

    /** The [DbEvaluationTemplate] this [DbTaskGroup] belongs to. */
    var evaluation: DbEvaluationTemplate by xdParent<DbTaskGroup, DbEvaluationTemplate>(DbEvaluationTemplate::taskGroups)

    /**
     * Converts this [DbTargetType] to a RESTful API representation [ApiTargetType].
     *
     * This is a convenience method and it requires and active transaction context.
     *
     * @return [ApiTargetType]
     */
    fun toApi(): ApiTaskGroup = ApiTaskGroup(this.name,this.type.name)
}
