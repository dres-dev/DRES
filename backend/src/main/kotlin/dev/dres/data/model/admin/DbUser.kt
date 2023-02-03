package dev.dres.data.model.admin

import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.PersistentEntity
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.simple.length
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredStringProp

typealias UserId = String

/**
 * A [DbUser] in the DRES user management model.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DbUser(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbUser>() {
        /** The minimum length of a password. */
        const val MIN_LENGTH_PASSWORD = 6

        /** The minimum length of a username. */
        const val MIN_LENGTH_USERNAME = 4
    }

    /** The [UserId] of this [DbUser]. */
    var userId: UserId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbUser]. Must be unique!*/
    var username by xdRequiredStringProp(unique = true, trimmed = true) { length(MIN_LENGTH_USERNAME, 16, "Username must consist of between 4 and 16 characters")}

    /** The password held by this [DbUser]. */
    var password by xdRequiredStringProp(unique = false, trimmed = true)

    /** The [DbRole] of this [DbUser]. */
    var role by xdLink1(DbRole)

    /**
     * The [Password.Hashed] held by this [DbUser].
     */
    fun hashedPassword() = Password.Hashed(this.password)

    override fun toString(): String = "User(id=$id, username=${username}, role=$role)"

    /**
     * Converts this [DbUser] to a RESTful API representation [ApiUser].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiUser]
     */
    fun toApi() = ApiUser(id = this.userId, username = this.username, role = this.role.toApi())
}