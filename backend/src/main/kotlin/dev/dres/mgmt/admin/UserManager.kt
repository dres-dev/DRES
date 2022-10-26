package dev.dres.mgmt.admin

import dev.dres.api.rest.handler.UserRequest
import dev.dres.data.model.UID
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

    const val MIN_LENGTH_PASSWORD = 6

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
     * Updates a [User] with the given [UID], [username], [password] and [role].
     *
     * @param id The [UID] of the user to update.
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Hashed] of the user.
     * @param role The [Role] of the new user.
     */
    fun update(id: UID?, username: String?, password: Password.Hashed?, role: Role?): Boolean = this.store.transactional {
        val user = if (id != null) {
            User.query(User::id eq id.string).firstOrNull()
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
     * Updates a [User] with the given [UID], [username], [password] and [role].
     *
     * @param id The [UID] of the user to update.
     * @param username The name of the [User]. Must be unique.
     * @param password The [Password.Plain] of the user.
     * @param role The [Role] of the new user.
     */
    fun update(id: UID?, username: String?, password: Password.Plain?, role: Role?): Boolean
        = update(id, username, password?.hash(), role)

    /**
     * Updates a [User] for the given [id] based o the [request].
     *
     * @param id The [UID] of the user to update.
     * @param request The [UserRequest] detailing the update
     * @return True on success, false otherwise.
     */
    fun update(id: UID?, request: UserRequest): Boolean
        = update(id = id, username = request.username, password = request.password?.let { Password.Plain(it) }, role = request.role?.let { Role.fromRestRole(it) })

    /**
     * Deletes the [User] for the given [UID].
     *
     * @param username The name of the [User] to delete.
     * @return True on success, false otherwise.
     */
    fun delete(username: String): Boolean = this.store.transactional {
        val user = User.query(User::username eq username).firstOrNull()
        if (user != null) {
            user.delete()
            true
        } else {
            false
        }
    }

    /**
     * Deletes the [User] for the given [UID].
     *
     * @param id The [UID] of the [User] to delete.
     * @return True on success, false otherwise.
     */
    fun delete(id: UID):Boolean = this.store.transactional {
        val user = User.query(User::id eq id.string).firstOrNull()
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
     * Checks for the existence of the [User] with the given [UID].
     *
     * @param id [UID] to check.
     * @return True if [User] exists, false otherwise.
     */
    fun exists(id: UID): Boolean = this.store.transactional(readonly = true) {
        User.query(User::id eq id.string).isNotEmpty
    }

    /**
     * Checks for the existence of the [User] with the given [UID].
     *
     * @param username User name to check.
     * @return True if [User] exists, false otherwise.
     */
    fun exists(username: String): Boolean = this.store.transactional(readonly = true) {
        User.query(User::username eq username).isNotEmpty
    }

    /**
     * Returns the [User] for the given [UID] or null if [User] doesn't exist.
     *
     * @param id The [UID] of the [User] to fetch.
     * @return [User] or null
     */
    fun get(id: UID): User? = this.store.transactional(readonly = true) {
        User.query(User::id eq id.string).firstOrNull()
    }

    /**
     * Returns the [User] for the given [username] or null if [User] doesn't exist.
     *
     * @param username The name of the [User] to fetch.
     * @return [User] or null
     */
    fun get(username: String): User? = this.store.transactional(readonly = true) {
        User.query(User::username eq username).firstOrNull()
    }

    /**
     * Either returns a user for this username/password tuple or null
     */
    fun getMatchingUser(username: String, password: Password.Plain) : User?  {
        val user = get(username)
        return if (user?.hashedPassword()?.check(password) == true) user else null
    }
}
