package dev.dres.mgmt.admin

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.api.rest.types.users.ApiUserRequest
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
     * @param username The name of the user. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [ApiRole] of the new user.
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
     * Updates a user with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the user. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [ApiRole] of the new user.
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
     * Updates a user for the given [id] based o the [request].
     *
     * @param id The [UserId] of the user to update.
     * @param request The [ApiUserRequest] detailing the update
     * @return True on success, false otherwise.
     */
    fun update(id: UserId?, request: ApiUserRequest): ApiUser? = update(
        id = id,
        username = request.username,
        password = request.password?.let { if (it.isNotBlank()) Password.Plain(it).hash() else null },
        role = request.role
    )

    /**
     * Deletes the [ApiUser] for the given [UserId].
     *
     * @param username The name of the [ApiUser] to delete.
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
     * Checks for the existence of the [ApiUser] with the given [EvaluationId].
     *
     * @param id [EvaluationId] to check.
     * @return True if [ApiUser] exists, false otherwise.
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
     * Returns the [ApiUser] for the given [EvaluationId] or null if [ApiUser] doesn't exist.
     *
     * @param id The [EvaluationId] of the [ApiUser] to fetch.
     * @return [ApiUser] or null
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
