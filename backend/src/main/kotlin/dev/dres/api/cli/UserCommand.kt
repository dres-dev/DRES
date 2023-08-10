package dev.dres.api.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.jakewharton.picnic.table
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.Password
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.DbUser.Companion.MIN_LENGTH_PASSWORD
import dev.dres.data.model.admin.DbUser.Companion.MIN_LENGTH_USERNAME
import dev.dres.data.model.admin.UserId
import dev.dres.mgmt.admin.UserManager
import jetbrains.exodus.database.TransientEntityStore
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * A collection of [CliktCommand]s for [DbUser] management
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class UserCommand(store: TransientEntityStore) : NoOpCliktCommand(name = "user") {
    init {
        this.subcommands(Create(store), Update(store), Delete(store), List(store), Roles(store), Export(store), Import(store))
    }

    override fun aliases() = mapOf(
        "ls" to listOf("list"),
        "remove" to listOf("delete"),
        "rm" to listOf("delete"),
        "drop" to listOf("delete"),
        "add" to listOf("create")
    )

    /**
     * [CliktCommand] to create a new [DbUser].
     */
    inner class Create(private val store: TransientEntityStore): CliktCommand(name = "create", help = "Creates a new User", printHelpOnEmptyArgs = true) {
        /** The name of the newly created user. */
        private val username: String by option("-u", "--username", help = "Username of at least $MIN_LENGTH_USERNAME characters length. Must be unique!")
                .required()
                .validate { require(it.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        /** The [Password.Plain] of the newly created user. */
        private val password: Password.Plain by option("-p", "--password", help = "Password of at least $MIN_LENGTH_PASSWORD characters length.")
                .convert { Password.Plain(it) }
                .required()
                .validate { require(it.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        /** The desired [DbRole] of the newly created user. */
        private val apiRole: ApiRole by option("-r", "--role", help = "Role of the new user.").convert { ApiRole.valueOf(it) }.required()

        override fun run() {
            try {
                val created = UserManager.create(username = this.username, password = this.password.hash(), role = apiRole)
                println("New user '${UserManager.get(username = this.username)}' created.")
            } catch (e: Exception) {
                println("Could not create user '${this.username}': ${e.message}")
            }

        }

    }

    /**
     * [CliktCommand] to update an existing [DbUser].
     */
    inner class Update(private val store: TransientEntityStore): CliktCommand(name = "update", help = "Updates Password or Role of an existing User", printHelpOnEmptyArgs = true) {
        private val id: UserId? by option("-i", "--id")

        /** The new username. */
        private val username: String? by option("-u", "--username", help = "Username of the user to be updated")
                .validate { require(it.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        /** The new [Password.Plain] of the updated user. Left unchanged if null! */
        private val password: Password.Plain? by option("-p", "--password", help = "New Password of at least $MIN_LENGTH_PASSWORD characters length")
                .convert { Password.Plain(it) }
                .validate { require(it.password.length >= MIN_LENGTH_PASSWORD) { "Password for DRES password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        /** The new [DbRole] of the updated  user. Left unchanged if null! */
        private val role: ApiRole? by option("-r", "--role", help = "New user Role").convert { ApiRole.parse(it) }

        override fun run()  {
            if (this.id == null && this.username == null) {
                println("You must specify a valid username or user ID in order to update a user!")
                return
            }
            if (UserManager.update(id = this.id, username = this.username, password = this.password?.hash(), role = this.role) != null) {
                println("User updated successfully!")
            } else {
                println("User updated failed. It probably doesn't exist!")
            }
        }
    }

    /**
     * [CliktCommand] to delete a [DbUser].
     */
    inner class Delete(private val store: TransientEntityStore): CliktCommand(name = "delete", help = "Deletes an existing user.", printHelpOnEmptyArgs = true) {
        private val id: UserId? by option("-i", "--id", help = "ID of the user to be deleted.")
        private val username: String? by option("-u", "--username", help = "Username of the user to be deleted.")
                .validate { require(it.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }

        override fun run() = this.store.transactional {
            if (this.id == null && this.username == null) {
                println("You must specify a valid username or user ID in order to delete a user!")
            }
            val success = UserManager.delete(id = this.id, username = this.username)
            if (success) {
                println("User deleted successfully!")
                return@transactional
            } else {
                println("User could not be deleted because it doesn't exist!")
            }
        }
    }

    /**
     * [CliktCommand] to export a [DbUser].
     */
    inner class Export(private val store: TransientEntityStore): CliktCommand(name = "export", help =  "Exports one or multiple user(s) as JSON.", printHelpOnEmptyArgs = true) {
        private val id: UserId? by option("-i", "--id", help = "ID of the user to be exported.")
        private val username: String? by option("-u", "--username", help = "Username of the user to be exported.")
                .validate { require(it.length >= MIN_LENGTH_USERNAME) { "Username for DRES user must consist of at least $MIN_LENGTH_USERNAME characters." } }
        private val path: String by option("-o", "--output").required()
        override fun run() = this.store.transactional(true) {
            if (this.id == null && this.username == null) {
                val users = UserManager.list()
                val path = Paths.get(this.path)
                val mapper = ObjectMapper()
                Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { writer ->
                    mapper.writeValue(writer, users)
                }
                println("Successfully wrote ${users.size} users to $path.")
                return@transactional
            } else {
                val user = UserManager.get(id = this.id, username = this.username)
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
     * Imports a specific user(s) from JSON.
     */
    inner class Import(private val store: TransientEntityStore): CliktCommand(name = "import", help = "Imports a user description from JSON. Either a single user or an array of users", printHelpOnEmptyArgs = true) {

        private val new: Boolean by option("-n", "--new", help = "Flag indicating whether users should be created anew.").flag("-u", "--update", default = true)

        private val multiple: Boolean by option("-m", "-multiple", help = "Flag indicating whether multiple users should be  imported.").flag("-s", "--single", default = true)

        private val destination: String by option("-i", "--in", help = "The input file for the users.").required()

        override fun run() = this.store.transactional {
            val path = Paths.get(this.destination)
            val mapper = ObjectMapper()

            val import = Files.newBufferedReader(path).use {
                if (this.multiple) {
                    mapper.readValue(it, Array<DbUser>::class.java)
                } else {
                    arrayOf(mapper.readValue(it, DbUser::class.java))
                }
            }

            import.forEach {
                if (new) {
                    UserManager.create(it.username, it.hashedPassword(), it.role.toApi())
                } else {
                    UserManager.update(it.id, it.username, it.hashedPassword(), it.role.toApi())
                }
            }
            println("done")
        }
    }


    /**
     * [CliktCommand] to list all [DbUser]s.
     */
    inner class List(private val store: TransientEntityStore): CliktCommand(name = "list", help = "Lists all Users") {
        val plain by option("-p", "--plain", help = "Plain print: No fancy table. Might be easier if the output should be processed").flag(default = false)
        override fun run() = this.store.transactional(true) {
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
                                row(it.id, it.username, it.role)
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * [CliktCommand] to list all [DbRole]s.
     */
    inner class Roles(private val store: TransientEntityStore): CliktCommand(name = "roles", help = "Lists all Roles") {
        override fun run() = this.store.transactional(true) {
            println("Available roles: ${DbRole.values().joinToString(", ")}")
        }
    }
}
