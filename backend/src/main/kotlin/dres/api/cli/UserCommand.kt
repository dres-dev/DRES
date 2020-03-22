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
import org.mindrot.jbcrypt.BCrypt

/**
 * A collection of [CliktCommand]s for user management
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class UserCommand (val users: DAO<User>) : NoOpCliktCommand(name = "users") {

    companion object {
        const val MIN_LENGTH_USERNAME = 4
        const val MIN_LENGTH_PASSWORD = 6
    }

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
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES user must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role by option("-r", "--role").enum<Role>().required()

        override fun run() {
            val newUser = User(username = this.username, password = this.password.hash(), role = role)
            for (existingUser in this@UserCommand.users) {
                if (existingUser.username == newUser.username) {
                    println("Could not create user '${newUser}' because a user with that name already exists.")
                    return
                }
            }
            this@UserCommand.users.append(newUser)
            println("New user '${newUser}' created.")
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
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES user must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role? by option("-r", "--role").enum<Role>()

        override fun run() {
            val updateId = when {
                this.id != null -> this.id!!
                this.username != null -> this@UserCommand.users.find { it.username == this.username!! }?.id
                else -> null
            }

            if (updateId != null) {
                val currentUser = this@UserCommand.users[updateId]
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(id = currentUser.id, username = currentUser.username, password = this.password?.hash() ?: currentUser.password, role = this.role ?: currentUser.role )
                    this@UserCommand.users.update(updatedUser)
                    println("User $updateId updated successfully (old: $currentUser, new: $updatedUser)!")
                } else {
                    println("User $updateId could not be updated because it doesn't exist!")
                }
            } else {
                println("You must specify a valid username or user ID in order to delete a user!")
            }
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
            val id = when {
                this.id != null -> this.id!!
                this.username != null -> this@UserCommand.users.find { it.username == this.username!! }?.id
                else -> null
            }

            if (id != null) {
                if (this@UserCommand.users.exists(id)) {
                    val user = this@UserCommand.users.delete(id)
                    println("User $user deleted successfully!")
                } else {
                    println("User with ID $id could not be deleted because it doesn't exist!")
                }
            } else {
                println("You must specify a valid username or user ID in order to delete a user!")
            }
        }
    }

    /**
     * [CliktCommand] to list all [User]s.
     */
    inner class ListUsers : CliktCommand(name = "list") {
        override fun run() {
            println("Available users:")
            for (user in this@UserCommand.users) {
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