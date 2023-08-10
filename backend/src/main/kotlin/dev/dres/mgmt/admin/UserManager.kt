package dev.dres.mgmt.admin

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.api.rest.types.users.UserRequest
import dev.dres.data.model.admin.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.util.*

/**
 * User management class of DRES. Requires transaction context.
 *
 * @author Loris Sauter
 * @version 2.0.1
 */
object UserManager {

    private lateinit var store: TransientEntityStore

    fun init(store: TransientEntityStore) {
        this.store = store
    }

    /**
     * Creates a [DbUser] with the given [username], [password] and [role].
     *
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun create(username: String, password: Password.Hashed, role: ApiRole): ApiUser =
        this.store.transactional {
            val dbUser =
                DbUser.new {
                    this.username = username.lowercase()
                    this.password = password.password
                    this.role = role.toDb() ?: throw IllegalArgumentException("Cannot create user with role '$role'")
                }
            return@transactional dbUser.toApi()
        }

    /**
     * Updates a [DbUser] with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the [DbUser]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [DbRole] of the new user.
     */
    fun update(id: UserId?, username: String?, password: Password.Hashed?, role: ApiRole?): ApiUser? =
        this.store.transactional {
            val user = if (id != null) {
                DbUser.query(DbUser::id eq id).firstOrNull()
            } else if (username != null) {
                DbUser.query(DbUser::username eq username).firstOrNull()
            } else {
                null
            }
            if (user == null) return@transactional null
            if (username != null) user.username = username.lowercase()
            if (password != null) user.password = password.password
            if (role != null) user.role = role.toDb() ?: throw IllegalArgumentException("Cannot update user to role '$role'")
            return@transactional user.toApi()
        }


    /**
     * Updates a [DbUser] for the given [id] based o the [request].
     *
     * @param id The [UserId] of the user to update.
     * @param request The [UserRequest] detailing the update
     * @return True on success, false otherwise.
     */
    fun update(id: UserId?, request: UserRequest): ApiUser? = update(
        id = id,
        username = request.username,
        password = request.password?.let { if (it.isNotBlank()) Password.Plain(it).hash() else null },
        role = request.role
    )

    /**
     * Deletes the [DbUser] for the given [UserId].
     *
     * @param username The name of the [DbUser] to delete.
     * @return True on success, false otherwise.
     */
    fun delete(id: UserId? = null, username: String? = null): Boolean = this.store.transactional {
        val user = if (id != null) {
            DbUser.query(DbUser::id eq id).firstOrNull()
        } else if (username != null) {
            DbUser.query(DbUser::username eq username.lowercase()).firstOrNull()
        } else {
            null
        }
        return@transactional if (user != null) {
            user.delete()
            true
        } else {
            false
        }
    }

    /**
     * Lists all [DbUser] objects in DRES.
     *
     * @return List of all [DbUser]s.
     */
    fun list(): List<ApiUser> = this.store.transactional(true) { DbUser.all().toList().map { it.toApi() } }


    /**
     * Checks for the existence of the [DbUser] with the given [EvaluationId].
     *
     * @param id [EvaluationId] to check.
     * @return True if [DbUser] exists, false otherwise.
     */
    fun exists(id: UserId? = null, username: String? = null): Boolean = this.store.transactional(true) {
        if (id != null) {
            DbUser.query(DbUser::id eq id).isNotEmpty
        } else if (username != null) {
            DbUser.query(DbUser::username eq username.lowercase()).isNotEmpty
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
    fun get(id: UserId? = null, username: String? = null): ApiUser? = this.store.transactional(true) {
        if (id != null) {
            DbUser.query(DbUser::id eq id).firstOrNull()?.toApi()
        } else if (username != null) {
            // Note: during after create, the query below is empty within a readonly transaction (unexpected), but non-empty out of the transaction
            DbUser.query(DbUser::username eq username.lowercase()).firstOrNull()?.toApi()
        } else {
            null
        }
    }

    /**
     * Either returns a user for this username/password tuple or null
     */
    fun getMatchingApiUser(username: String, password: Password.Plain): ApiUser? = this.store.transactional(true) {
        val user = DbUser.query(DbUser::username eq username.lowercase()).firstOrNull()
        if (user?.hashedPassword()?.check(password) == true) user.toApi() else null
    }
}
