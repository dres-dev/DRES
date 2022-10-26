package dev.dres.data.model.admin

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.UID
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.simple.length
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredStringProp

typealias UserId = UID

/**
 * A [User] in the DRES user management model.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class User(entity: Entity): PersistentEntity(entity) {
    companion object : XdNaturalEntityType<User>()

    /** The name held by this [User]. Must be unique!*/
    var username by xdRequiredStringProp(unique = true, trimmed = false) { length(4, 16, "Username must consist of between 4 and 16 characters")}

    /** The password held by this [User]. */
    var password by xdRequiredStringProp(unique = false, trimmed = false)

    /** The [Role] of this [User]. */
    var role by xdLink1(Role)

    /**
     * The [Password.Hashed] held by this [User].
     */
    fun hashedPassword() = Password.Hashed(this.password)

    override fun toString(): String = "User(id=$id, username=${username}, role=$role)"
}