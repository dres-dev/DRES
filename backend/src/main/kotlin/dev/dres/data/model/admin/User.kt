package dev.dres.data.model.admin

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.team.Team
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.simple.length
import kotlinx.dnq.xdLink0_N
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredStringProp

typealias UserId = String

/**
 * A [User] in the DRES user management model.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class User(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<User>() {
        /** The minimum length of a password. */
        const val MIN_LENGTH_PASSWORD = 6

        /** The minimum length of a username. */
        const val MIN_LENGTH_USERNAME = 4
    }

    /** The [UserId] of this [User]. */
    var userId: UserId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [User]. Must be unique!*/
    var username by xdRequiredStringProp(unique = true, trimmed = false) { length(MIN_LENGTH_USERNAME, 16, "Username must consist of between 4 and 16 characters")}

    /** The password held by this [User]. */
    var password by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [Role] of this [User]. */
    var role by xdLink1(Role)

    /** The [Team]s this [User] belongs to. */
    val teams by xdLink0_N<User, Team>(Team::users, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The [CompetitionDescription]s this [User] acts as judge for. */
    val judges by xdLink0_N(CompetitionDescription::judges, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * The [Password.Hashed] held by this [User].
     */
    fun hashedPassword() = Password.Hashed(this.password)

    override fun toString(): String = "User(id=$id, username=${username}, role=$role)"
}