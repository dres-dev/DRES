package dev.dres.api.cli

import dev.dres.api.rest.types.users.ApiRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class UserCsvImportTest {
    @Test
    fun `parses user rows and case-insensitive headers`() {
        val csv = """Username,Password,Role
alice1,secret1,ADMIN
"""

        val users = UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, users.size)
        assertEquals(2, users.single().rowNumber)
        assertEquals("alice1", users.single().user.username)
        assertEquals("secret1", users.single().user.password)
        assertEquals(ApiRole.ADMIN, users.single().user.role)
    }

    @Test
    fun `rejects missing required columns`() {
        val csv = """username,password
alice1,secret1
"""

        val error = assertThrows(IllegalArgumentException::class.java) {
            UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))
        }

        assertEquals("CSV is missing required column(s): role. Required headers: username,password,role", error.message)
    }

    @Test
    fun `rejects invalid role with source row number`() {
        val csv = """username,password,role
alice1,secret1,ANYONE
"""

        val error = assertThrows(IllegalArgumentException::class.java) {
            UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))
        }

        assertEquals("CSV row 2: invalid role 'ANYONE'", error.message)
    }

    @Test
    fun `accepts an email username longer than the old limit`() {
        val username = "razvan.dobasu2@mail.dcu.ie"
        val csv = """username,password,role
$username,secret1,ADMIN
"""

        val users = UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(username, users.single().user.username)
    }

    @Test
    fun `accepts usernames up to the database limit`() {
        val username = "a".repeat(254)
        val csv = """username,password,role
$username,secret1,ADMIN
"""

        val users = UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(username, users.single().user.username)
    }

    @Test
    fun `rejects username above database length limit`() {
        val username = "a".repeat(255)
        val csv = """username,password,role
$username,secret1,ADMIN
"""

        val error = assertThrows(IllegalArgumentException::class.java) {
            UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))
        }

        assertEquals("CSV row 2: username must contain between 4 and 254 characters", error.message)
    }

    @Test
    fun `rejects short passwords before touching users`() {
        val csv = """username,password,role
alice1,short,ADMIN
"""

        val error = assertThrows(IllegalArgumentException::class.java) {
            UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))
        }

        assertEquals("CSV row 2: password must contain at least 6 characters", error.message)
    }

    @Test
    fun `rejects duplicate usernames ignoring case`() {
        val csv = """username,password,role
alice1,secret1,ADMIN
ALICE1,secret2,JUDGE
"""

        val error = assertThrows(IllegalArgumentException::class.java) {
            UserCsvImport.read(ByteArrayInputStream(csv.toByteArray()))
        }

        assertEquals("CSV contains duplicate usernames: alice1", error.message)
    }
}
