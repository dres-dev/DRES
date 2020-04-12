package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.long
import dres.data.dbo.DAO
import dres.data.model.admin.*
import dres.mgmt.admin.UserManager
import dres.mgmt.admin.UserManager.MIN_LENGTH_PASSWORD
import dres.mgmt.admin.UserManager.MIN_LENGTH_USERNAME

/**
 * A collection of [CliktCommand]s for user management
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class UserCommand() : NoOpCliktCommand(name = "users") {


    init {
        this.subcommands(CreateUserCommand(), UpdateUserCommand(), DeleteUserCommand(), ListUsers(), ListRoles())
    }

    /**
     * [CliktCommand] to create a new [User].
     */
    inner class CreateUserCommand : CliktCommand(name = "create") {
        private val username: UserName by option("-u", "--username")
                .convert { UserName(it) }
                .required()
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        private val password: PlainPassword by option("-p", "--password")
                .convert { PlainPassword(it) }
                .required()
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role by option("-r", "--role").enum<Role>().required()

        override fun run() {
            val successful = UserManager.create(username = this.username, password = this.password, role = role)
            if (successful) {
                println("New user '${UserManager.get(username = this.username)}' created.")

            } else {
                println("Could not create user '${this.username}' because a user with that name already exists.")

            }
        }
    }

    /**
     * [CliktCommand] to update an existing [User].
     */
    inner class UpdateUserCommand : CliktCommand(name = "update") {
        private val id: Long? by option("-i", "--id").long()
        private val username: UserName? by option("-u", "--username")
                .convert { UserName(it) }
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        private val password: PlainPassword? by option("-p", "--password")
                .convert { PlainPassword(it) }
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role? by option("-r", "--role").enum<Role>()

        override fun run() {
            if (UserManager.id(id = this.id, username = this.username) == null) {
                println("You must specify a valid username or user ID in order to update a user!")
                return
            }
            if (UserManager.exists(id = this.id, username = this.username)) {
                val userId = UserManager.id(id = id, username = username)!!
                val currentUser = UserManager.get(id = userId)
                val success = UserManager.update(id = id, username = username, password = password, role = role)
                if (success) {
                    val updatedUser = UserManager.get(id = id, username = username)
                    println("User $userId updated successfully (old: $currentUser, new: $updatedUser)!")
                    return
                }
            }

            println("User[id=${id ?: "N/A"},username=${username
                    ?: "N/A"}] could not be updated because it doesn't exist!")
        }
    }

    /**
     * [CliktCommand] to delete a [User].
     */
    inner class DeleteUserCommand : CliktCommand(name = "delete") {
        private val id: Long? by option("-i", "--id").long()
        private val username: UserName? by option("-u", "--username")
                .convert { UserName(it) }
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        override fun run() {
            val delId = UserManager.id(id = id, username = username)
            if (delId == null) {
                println("You must specify a valid username or user ID in order to delete a user!")
            }
            if (UserManager.exists(id = delId)) {
                val user = UserManager.get(delId)!! // !! okay, exists checks
                val success = UserManager.delete(id = id, username = username)
                if (success) {
                    println("User $user deleted successfully!")
                    return
                }
            }
            println("User with ID $delId could not be deleted because it doesn't exist!")
        }
    }

    /**
     * [CliktCommand] to list all [User]s.
     */
    inner class ListUsers : CliktCommand(name = "list") {
        override fun run() {
            println("Available users:")
            for (user in UserManager.list()) {
                println("$user")
            }
        }
    }

    /**
     * [CliktCommand] to list all [Role]s.
     */
    inner class ListRoles : CliktCommand(name = "roles") {
        override fun run() {
            println("Available roles: ${Role.values().joinToString(", ")}")
        }
    }
}