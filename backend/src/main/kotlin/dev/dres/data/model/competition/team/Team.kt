package dev.dres.data.model.competition.team

import dev.dres.api.rest.types.competition.ApiTeam
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.competition.CompetitionDescription
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.asSequence
import java.nio.file.Paths

/** The ID of a [Team]. */
typealias TeamId = String

/** The ID of a [Team]'s logo. */
typealias LogoId = String

/**
 * Represents a [Team] that takes part in a competition managed by DRES.
 *
 * @author Ralph Gasser, Loris Sauter, Luca Rossetto
 * @version 2.0.0
 */
class Team(entity: Entity) : PersistentEntity(entity) {
    companion object: XdNaturalEntityType<Team>() {
        /** Combination of [Team] name / competition must be unique. */
        override val compositeIndices = listOf(
            listOf(Team::name, Team::competition)
        )

        /**
         * Generates and returns the [Path] to the team logo with the given [logoId].
         *
         * @param config The global [Config] used to construct the [Path].
         * @param logoId The ID of the desired logo.
         */
        fun logoPath(config: Config, logoId: LogoId) = Paths.get(config.cachePath, "logos", "${logoId}.png")
    }

    /** The [TeamId] of this [Team]. */
    var teamId: TeamId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [Team]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = false)

    /** The password held by this [User]. */
    var color by xdRequiredStringProp(unique = false, trimmed = false)

    /** The password held by this [User]. */
    var logoId by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [CompetitionDescription] this [Team] belongs to. */
    var competition by xdParent<Team,CompetitionDescription>(CompetitionDescription::teams)

    /** The [TeamGroup] this [Team] belongs to (or null if not assigned to a group). */
    var group by xdLink0_1(TeamGroup::teams)

    /** The [User]s that belong to this [Team]. */
    val users by xdLink0_N(User, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Converts this [Team] to a RESTful API representation [ApiTeam].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeam]
     */
    fun toApi() = ApiTeam(this.name, this.color, this.logoId, this.users.asSequence().map { it.toApi() }.toList())
}