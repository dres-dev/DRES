package dev.dres.mgmt.admin

import dev.dres.api.rest.types.users.UserRequest
import dev.dres.data.model.admin.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

/**
 * User management class of DRES. Requires one-time initialisation
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
object UserManager {

    /** The [TransientEntityStore] instance used by this [UserManager]. */
    private lateinit var store: TransientEntityStore

    fun init(store: TransientEntityStore) {
        this.store = store
    }

    /**
     * Creates a [User] with the given [username], [password] and [role].
     *
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [Role] of the new user.
     */
    fun create(username: String, password: Password.Hashed, role: Role): Boolean {
        check(::store.isInitialized) { "PUserManager requires an initialized store which is unavailable. This is a programmer's error!"}
        try {
            this.store.transactional {
                User.new {
                    this.username = username
                    this.password = password.password
                    this.role = role
                }
            }
        } catch (e: Throwable) {
            return false
        }
        return true
    }

    /**
     * Creates a [User] with the given [username], [password] and [role].
     *
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Plain] of the user.
     * @param role The [Role] of the new user.
     */
    fun create(username: String, password: Password.Plain, role: Role): Boolean {
        check(::store.isInitialized) { "UserManager requires an initialized store which is unavailable. This is a programmer's error!"}
        return create(username, password.hash(), role)
    }

    /**
     * Updates a [User] with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [Role] of the new user.
     */
    fun update(id: UserId?, username: String?, password: Password.Hashed?, role: Role?): Boolean = this.store.transactional {
        val user = if (id != null) {
            User.query(User::id eq id).firstOrNull()
        } else if (username != null) {
            User.query(User::username eq username).firstOrNull()
        } else {
            null
        }
        if (user == null) return@transactional false
        if (username != null) user.username = username
        if (password != null) user.password = password.password
        if (role != null) user.role = role
        true
    }

    /**
     * Updates a [User] with the given [UserId], [username], [password] and [role].
     *
     * @param id The [UserId] of the user to update.
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Plain] of the user.
     * @param role The [Role] of the new user.
     */
    fun update(id: UserId?, username: String?, password: Password.Plain?, role: Role?): Boolean
        = update(id, username, password?.hash(), role)

    /**
     * Updates a [User] for the given [id] based o the [request].
     *
     * @param id The [UserId] of the user to update.
     * @param request The [UserRequest] detailing the update
     * @return True on success, false otherwise.
     */
    fun update(id: UserId?, request: UserRequest): Boolean
        = update(id = id, username = request.username, password = request.password?.let { Password.Plain(it) }, role = request.role?.let { Role.convertApiRole(it) })

    /**
     * Deletes the [User] for the given [UserId].
     *
     * @param username The name of the [User] to delete.
     * @return True on success, false otherwise.
     */
    fun delete(id: UserId? = null, username: String? = null): Boolean = this.store.transactional {
        val user = if (id != null) {
            User.query(User::id eq id).firstOrNull()
        } else if (username != null) {
            User.query(User::username eq username).firstOrNull()
        } else {
            null
        }
        if (user != null) {
            user.delete()
            true
        } else {
            false
        }
    }

    /**
     * Lists all [User] objects in DRES.
     *
     * @return List of all [User]s.
     */
    fun list(): List<User> = this.store.transactional(readonly = true) {
        User.all().toList()
    }

    /**
     * Checks for the existence of the [User] with the given [EvaluationId].
     *
     * @param id [EvaluationId] to check.
     * @return True if [User] exists, false otherwise.
     */
    fun exists(id: UserId? = null, username: String? = null): Boolean = this.store.transactional(readonly = true) {
        if (id != null) {
            User.query(User::id eq id).isNotEmpty
        } else if (username != null) {
            User.query(User::username eq username).isNotEmpty
        } else {
            throw IllegalArgumentException("Either user ID or username must be non-null!")
        }
    }

    /**
     * Returns the [User] for the given [EvaluationId] or null if [User] doesn't exist.
     *
     * @param id The [EvaluationId] of the [User] to fetch.
     * @return [User] or null
     */
    fun get(id: UserId? = null, username: String? = null): User? = this.store.transactional(readonly = true) {
        if (id != null) {
            User.query(User::id eq id).firstOrNull()
        } else if (username != null) {
            User.query(User::username eq username).firstOrNull()
        } else {
            null
        }
    }

    /**
     * Either returns a user for this username/password tuple or null
     */
    fun getMatchingUser(username: String, password: Password.Plain) : User?  {
        val user = get(null, username)
        return if (user?.hashedPassword()?.check(password) == true) user else null
    }
}
