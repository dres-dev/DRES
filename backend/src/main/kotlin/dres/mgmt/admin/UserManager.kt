package dres.mgmt.admin

import dres.api.rest.handler.UserHandler
import dres.data.dbo.DAO
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.utilities.extensions.toPlainPassword
import dres.utilities.extensions.toUsername

/**
 * User management of DRES.
 * Single access to DAO for users. Requires initialisation ONCE
 *
 * @author Loris Sauter
 * @version 1.0
 */
object UserManager {

    const val MIN_LENGTH_USERNAME = 4
    const val MIN_LENGTH_PASSWORD = 6

    private lateinit var users: DAO<User>


    fun init(users: DAO<User>) {
        this.users = users
    }

    fun create(username: UserName, password: PlainPassword, role: Role): Boolean {
        validateInitalised()
        if(password.length < MIN_LENGTH_PASSWORD){
            throw RuntimeException("Password is less than $MIN_LENGTH_PASSWORD characters")
        }
        if(username.length < MIN_LENGTH_USERNAME){
            throw RuntimeException("Username is less than $MIN_LENGTH_USERNAME characters")
        }
        val newUser = User(username = username, password = password.hash(), role = role)
        for (existingUser in this.users) {
            if (existingUser in users) {
                return false
            }
        }
        users.append(newUser)
        return true
    }

    fun update(id: Long?, username: UserName?, password: PlainPassword?, role: Role?): Boolean {
        validateInitalised()
        val updateId = id(id, username)
        if (updateId != null) {
            val currentUser = users[updateId]
            if (currentUser != null) {
                val updatedUser = currentUser.copy(id = currentUser.id, username = username ?: currentUser.username, password = password?.hash()
                        ?: currentUser.password, role = role ?: currentUser.role)
                users.update(updatedUser)
                return true
            }
        }
        return false
    }

    fun delete(id: Long?, username: UserName?): Boolean {
        validateInitalised()
        val delId = id(id, username)
        if (delId != null && exists(delId)) {
            this.users.delete(delId)
            return true
        } else {
            return false
        }
    }

    fun delete(id:Long?):Boolean{
        return delete(id=id,username = null)
    }

    fun list(): List<User> {
        validateInitalised()
        // TODO is there a more elegant way?
        val ls = mutableListOf<User>()
        users.forEach { ls.add(it) }
        return ls
    }

    fun exists(id: Long?, username: UserName?): Boolean {
        validateInitalised()
        val searchId = id(id, username)
        return this.users.exists(searchId ?: Long.MIN_VALUE)
    }

    fun exists(id: Long?): Boolean {
        return exists(id = id, username = null)
    }

    fun exists(username: UserName?): Boolean {
        return exists(id = null, username = username)
    }

    fun get(id: Long?, username: UserName?): User? {
        validateInitalised()
        val _id = id(id, username)
        return if (exists(id = _id)) {
            users[_id!!] // !! is safe here, because theres a nulll check in exists
        } else {
            null
        }
    }

    fun get(id: Long?): User? {
        return get(id = id, username = null)
    }

    fun get(username: UserName?): User? {
        return get(id = null, username = username)
    }

    /**
     * Returns the id
     *   if the id is not null
     *   if the username is not null, the corresponding id
     *   null if failed
     */
    fun id(id: Long?, username: UserName?): Long? {
        return when {
            id != null -> id
            username != null -> users.find { it.username == username }?.id
            else -> null
        }
    }

    private fun validateInitalised() {
        if (isInit().not()) {
            throw RuntimeException("The UserManager was not initialised with a DAO")
        }
    }

    private fun isInit(): Boolean {
        return ::users.isInitialized
    }

    fun create(toCreate: UserHandler.UserRequest): Boolean {
        return create(UserName(toCreate.username), if(toCreate.password != null){PlainPassword(toCreate.password)}else{
            PlainPassword("")
        }, toCreate.role!!)
    }

    fun updateEntirely(id:Long?, user: UserHandler.UserRequest): Boolean {
        return update(id=id, username = user.username.toUsername(), password = user.password.toPlainPassword(), role = user.role)
    }

    fun update(id:Long?, user:UserHandler.UserRequest):Boolean{
        return update(id=id, username = user.username.toUsername(), password = user.password.toPlainPassword(), role=null)
    }
}
