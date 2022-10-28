package dev.dres.data.model.competition.team

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.competition.CompetitionDescription
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

typealias TeamGroupId = String

/**
 * Represents a [TeamGroup] that takes part in a competition managed by DRES.
 *
 * Can be used to aggregate score values across [Team]s
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
class TeamGroup(entity: Entity) : PersistentEntity(entity) {

    companion object: XdNaturalEntityType<TeamGroup>()

    /** The [UserId] of this [User]. */
    var teamGroupId: TeamGroupId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [User]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = false)

    /** The default [TeamAggregator] to use for this [TeamGroup]. */
    var defaultAggregator by xdLink1(TeamAggregator)

    /** The [CompetitionDescription] this [Team] belongs to. */
    var competition by xdParent<TeamGroup,CompetitionDescription>(CompetitionDescription::teamsGroups)

    /** The [Team]s that belong to this [TeamGroup]. */
    val teams by xdLink0_N<TeamGroup,Team>(Team::group)
}