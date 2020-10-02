package dev.dres.api.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.long
import com.jakewharton.picnic.table
import dev.dres.data.model.UID
import dev.dres.data.model.admin.PlainPassword
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserName
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.mgmt.admin.UserManager
import dev.dres.mgmt.admin.UserManager.MIN_LENGTH_PASSWORD
import dev.dres.mgmt.admin.UserManager.MIN_LENGTH_USERNAME
import dev.dres.utilities.extensions.UID
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * A collection of [CliktCommand]s for user management
 *
 * @author Ralph Gasser
 * @version 1.1
 */
class UserCommand : NoOpCliktCommand(name = "user") {


    init {
        this.subcommands(CreateUserCommand(), UpdateUserCommand(), DeleteUserCommand(), ListUsers(), ListRoles(), ExportUserCommand(), ImportUserCommand())
    }

    /**
     * [CliktCommand] to create a new [User].
     */
    inner class CreateUserCommand : CliktCommand(name = "create", help = "Creates a new User") {
        private val username: UserName by option("-u", "--username", help = "Username of at least $MIN_LENGTH_USERNAME characters length")
                .convert { UserName(it) }
                .required()
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        private val password: PlainPassword by option("-p", "--password", help = "Password of at least $MIN_LENGTH_PASSWORD characters length")
                .convert { PlainPassword(it) }
                .required()
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role by option("-r", "--role", help = "Role of the new User").enum<Role>().required()

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
    inner class UpdateUserCommand : CliktCommand(name = "update", help = "Updates Password or Role of an existing User") {
        private val id: UID? by option("-i", "--id").convert { it.UID() }
        private val username: UserName? by option("-u", "--username", help = "Username of the user to be updated")
                .convert { UserName(it) }
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        private val password: PlainPassword? by option("-p", "--password", help = "New Password of at least $MIN_LENGTH_PASSWORD characters length")
                .convert { PlainPassword(it) }
                .validate { require(it.pass.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        private val role: Role? by option("-r", "--role", help = "New user Role").enum<Role>()

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
    inner class DeleteUserCommand : CliktCommand(name = "delete", help = "Deletes an existing user.") {
        private val id: UID? by option("-i", "--id", help = "ID of the user to be deleted.").convert { it.UID() }
        private val username: UserName? by option("-u", "--username", help = "Username of the user to be deleted.")
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
     * [CliktCommand] to export a [User].
     */
    inner class ExportUserCommand : CliktCommand(name = "export", help =  "Exports one or multiple user(s) as JSON.") {
        private val id: UID? by option("-i", "--id", help = "ID of the user to be exported.").convert { it.UID() }
        private val username: UserName? by option("-u", "--username", help = "Username of the user to be exported.")
                .convert { UserName(it) }
                .validate { require(it.name.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }
        private val path: String by option("-o", "--output").required()
        override fun run() {
            val id = UserManager.id(id = id, username = username)
            if (id == null) {
                val users = UserManager.list()
                val path = Paths.get(this.path)
                val mapper = ObjectMapper()
                Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { writer ->
                    mapper.writeValue(writer, users)
                }
                println("Successfully wrote ${users.size} users to $path.")
                return
            } else {
                val user = UserManager.get(id)
                if (user != null) {
                    val path = Paths.get(this.path)
                    val mapper = ObjectMapper()
                    Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
                        mapper.writeValue(it, user)
                    }
                    println("Successfully wrote user ${user.id} to $path.")
                } else {
                    println("User with ID $id does not exist.")
                }
            }
        }
    }

    /**
     * Imports a specific competition from JSON.
     */
    inner class ImportUserCommand : CliktCommand(name = "import", help = "Imports a user description from JSON.") {

        private val new: Boolean by option("-n", "--new", help = "Flag indicating whether users should be created anew.").flag("-u", "--update", default = true)

        private val multiple: Boolean by option("-m", "-multiple", help = "Flag indicating whether multiple users should be  imported.").flag("-s", "--single", default = true)

        private val destination: String by option("-i", "--in", help = "The input file for the users.").required()

        override fun run() {
            val path = Paths.get(this.destination)
            val mapper = ObjectMapper()

            val import = Files.newBufferedReader(path).use {
                if (this.multiple) {
                    mapper.readValue(it, Array<User>::class.java)
                } else {
                    arrayOf(mapper.readValue(it, User::class.java))
                }
            }

            import.forEach {
                if (new) {
                    UserManager.create(it.username, it.password, it.role)
                } else {
                    UserManager.update(it.id, it.username, it.password, it.role)
                }
            }
        }
    }


    /**
     * [CliktCommand] to list all [User]s.
     */
    inner class ListUsers : CliktCommand(name = "list", help = "Lists all Users") {
        val plain by option("-p", "--plain", help = "Plain print: No fancy table. Might be easier if the output should be processed").flag(default = false)
        override fun run() {
            val users = UserManager.list()
            println("Available users: ${users.size}")
            if (plain) {
                for (user in users) {
                    println("$user")
                }
            } else {
                println(
                        table {
                            cellStyle {
                                border = true
                                paddingLeft = 1
                                paddingRight = 1
                            }
                            header {
                                row("id", "username", "role")
                            }
                            body {
                                users.forEach {
                                    row(it.id, it.username.name, it.role)
                                }
                            }
                        }
                )
            }
        }
    }

    /**
     * [CliktCommand] to list all [Role]s.
     */
    inner class ListRoles : CliktCommand(name = "roles", help = "Lists all Roles") {
        override fun run() {
            println("Available roles: ${Role.values().joinToString(", ")}")
        }
    }
}