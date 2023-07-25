package dev.dres.data.model.template.team

import dev.dres.api.rest.types.template.team.ApiTeamGroup
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.template.DbEvaluationTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.toList

typealias TeamGroupId = String

/**
 * Represents a [DbTeamGroup] that takes part in a competition managed by DRES.
 *
 * Can be used to aggregate score values across [DbTeam]s
 *
 * @author Luca Rossetto & Loris Sauter
 * @version 1.0.0
 */
class DbTeamGroup(entity: Entity) : PersistentEntity(entity) {

    companion object: XdNaturalEntityType<DbTeamGroup>()

    /** The [UserId] of this [DbUser]. */
    var teamGroupId: TeamGroupId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbUser]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The default [DbTeamAggregator] to use for this [DbTeamGroup]. */
    var defaultAggregator by xdLink1(DbTeamAggregator)

    /** The [DbEvaluationTemplate] this [DbTeam] belongs to. */
    val evaluation: DbEvaluationTemplate by xdParent<DbTeamGroup,DbEvaluationTemplate>(DbEvaluationTemplate::teamGroups)

    /** The [DbTeam]s that belong to this [DbTeamGroup]. */
    val teams by xdLink1_N<DbTeamGroup,DbTeam>(DbTeam::groups)

    /**
     * Converts this [DbTeamGroup] to a RESTful API representation [ApiTeamGroup].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeamGroup]
     */
    fun toApi() = ApiTeamGroup(this.teamGroupId, this.name, this.teams.asSequence().map { it.toApi() }.toList(), this.defaultAggregator.toApi())

    /**
     * Returns a new [TeamAggregatorImpl] for this [DbTeamGroup].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [TeamAggregatorImpl]
     */
    fun newAggregator() : TeamAggregatorImpl = this.defaultAggregator.newInstance(this.teams.toList())
}
