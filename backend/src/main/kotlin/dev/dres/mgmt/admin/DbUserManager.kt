package dev.dres.mgmt.admin

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.api.rest.types.users.UserRequest
import dev.dres.data.model.admin.*
import kotlinx.dnq.query.*
import java.util.*

/**
 * User management class of DRES. Requires transaction context.
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
object DbUserManager {


    /**
     * Creates a [DbUser] with the given [username], [password] and [role].
     *
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun create(username: String, password: Password.Hashed, role: DbRole): Boolean {
        try {
                DbUser.new {
                    this.username = username
                    this.password = password.password
                    this.role = role
                }

        } catch (e: Throwable) {
            return false
        }
        return true
    }

    /**
     * Creates a [DbUser] with the given [username], [password] and [role].
     *
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [ApiRole] of the new user.
     */
    fun create(username: String, password: Password.Hashed, role: ApiRole): Boolean {
        return create(username, password, role.toDb() ?: throw IllegalArgumentException("Invalid Role"))
    }

    /**
     * Creates a [DbUser] with the given [username], [password] and [role].
     *
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Plain] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun create(username: String, password: Password.Plain, role: DbRole): Boolean {
        return create(username, password.hash(), role)
    }

    /**
     * Updates a [DbUser] with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun update(id: UserId?, username: String?, password: Password.Hashed?, role: DbRole?): Boolean {
        val user = if (id != null) {
            DbUser.query(DbUser::id eq id).firstOrNull()
        } else if (username != null) {
            DbUser.query(DbUser::username eq username).firstOrNull()
        } else {
            null
        }
        if (user == null) return false
        if (username != null) user.username = username
        if (password != null) user.password = password.password
        if (role != null) user.role = role
        return true
    }

    /**
     * Updates a [DbUser] with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Plain] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun update(id: UserId?, username: String?, password: Password.Plain?, role: DbRole?): Boolean
        = update(id, username, password?.hash(), role)

    /**
     * Updates a [DbUser] for the given [id] based o the [request].
     *
     * @param id The [UserId] of the user to update.
     * @param request The [UserRequest] detailing the update
     * @return True on success, false otherwise.
     */
    fun update(id: UserId?, request: UserRequest): Boolean
        = update(id = id, username = request.username, password = request.password?.let { Password.Plain(it) }, role = request.role?.toDb())

    /**
     * Deletes the [DbUser] for the given [UserId].
     *
     * @param username The name of the [DbUser] to delete.
     * @return True on success, false otherwise.
     */
    fun delete(id: UserId? = null, username: String? = null): Boolean {
        val user = if (id != null) {
            DbUser.query(DbUser::id eq id).firstOrNull()
        } else if (username != null) {
            DbUser.query(DbUser::username eq username).firstOrNull()
        } else {
            null
        }
        if (user != null) {
            user.delete()
            return true
        } else {
            return false
        }
    }

    /**
     * Lists all [DbUser] objects in DRES.
     *
     * @return List of all [DbUser]s.
     */
    fun list(): List<DbUser> = DbUser.all().toList()


    /**
     * Checks for the existence of the [DbUser] with the given [EvaluationId].
     *
     * @param id [EvaluationId] to check.
     * @return True if [DbUser] exists, false otherwise.
     */
    fun exists(id: UserId? = null, username: String? = null): Boolean {
        return if (id != null) {
            DbUser.query(DbUser::id eq id).isNotEmpty
        } else if (username != null) {
            DbUser.query(DbUser::username eq username).isNotEmpty
        } else {
            throw IllegalArgumentException("Either user ID or username must be non-null!")
        }
    }

    /**
     * Returns the [DbUser] for the given [EvaluationId] or null if [DbUser] doesn't exist.
     *
     * @param id The [EvaluationId] of the [DbUser] to fetch.
     * @return [DbUser] or null
     */
    fun get(id: UserId? = null, username: String? = null): DbUser?  {
        return if (id != null) {
            DbUser.query(DbUser::id eq id).firstOrNull()
        } else if (username != null) {
            // Note: during after create, the query below is empty within a readonly transaction (unexpected), but non-empty out of the transaction
            DbUser.query(DbUser::username eq username).firstOrNull()
        } else {
            null
        }
    }

    /**
     * Either returns a user for this username/password tuple or null
     */
    fun getMatchingApiUser(username: String, password: Password.Plain) : ApiUser? {
        val user = get(null, username)
        return if (user?.hashedPassword()?.check(password) == true) user.toApi() else null
    }
}
