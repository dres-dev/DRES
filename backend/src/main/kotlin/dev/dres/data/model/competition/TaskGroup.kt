package dev.dres.data.model.competition

import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdChildren0_N
import kotlinx.dnq.xdParent
import kotlinx.dnq.xdRequiredStringProp

/**
 * A [TaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class TaskGroup(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<TaskGroup>()

    /** The name of this [TaskGroup]. */
    var name: String by xdRequiredStringProp(false, false)

    var type: String by xdRequiredStringProp(false, false)

    /** The [CompetitionDescription] this [TaskGroup] belongs to. */
    var competition by xdParent<TaskGroup,CompetitionDescription>(CompetitionDescription::taskGroups)

    /** The [TaskDescription]s contained in this [TaskGroup]*/
    val tasks by xdChildren0_N<TaskGroup,TaskDescription>(TaskDescription::taskGroup)
}