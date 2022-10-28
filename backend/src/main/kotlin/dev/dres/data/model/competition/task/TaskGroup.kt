package dev.dres.data.model.competition.task

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.team.Team
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

    /** The [CompetitionDescription] this [TaskGroup] belongs to. */
    var competition by xdParent<TaskGroup, CompetitionDescription>(CompetitionDescription::taskGroups)

    /** The [TaskDescription]s contained in this [TaskGroup]*/
    val tasks by xdChildren0_N<TaskGroup, TaskDescription>(TaskDescription::taskGroup)
}