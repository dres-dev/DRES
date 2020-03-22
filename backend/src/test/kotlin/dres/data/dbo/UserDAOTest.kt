package dres.data.dbo

import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.serializers.UserSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class UserDAOTest {


    private val path = Paths.get("./user-test.db")

    private var dao : DAO<User>? = null

    @BeforeEach
    fun beforeEach() {
        this.dao = DAO(path, UserSerializer)
    }

    @AfterEach
    fun afterEach() {
        this.dao?.close()
        this.dao = null
        Files.delete(path)
    }

    /**
     * Tests the [DAO]s append method.
     */
    @Test
    fun testAppend() {
        val user1 = User(username = "rgasser", password = "12345", role = Role.ADMIN)
        val user2 = User(username = "lrossetto", password = "45678", role = Role.ADMIN)
        val user3 = User(username = "vbs2021", password = "abcde", role = Role.JUDGE)

        assertEquals(1L, this.dao!!.append(user1))
        assertEquals(2L, this.dao!!.append(user2))
        assertEquals(3L, this.dao!!.append(user3))


        assertEquals(user1, this.dao!![1L])
        assertEquals(user2, this.dao!![2L])
        assertEquals(user3, this.dao!![3L])
    }

    /**
     * Tests the [DAO]s append method.
     */
    @Test
    fun testIteration() {
        val users = arrayOf(
                User(username = "rgasser", password = "12345", role = Role.ADMIN),
                User(username = "lrossetto", password = "45678", role = Role.ADMIN),
                User(username = "vbs2021", password = "abcde", role = Role.JUDGE),
                User(username = "testUser1", password = "lalala", role = Role.VIEWER),
                User(username = "testUser2", password = "truestory!", role = Role.VIEWER)
        )


        for ((i, user) in users.withIndex()) {
            assertEquals(i+1L, this.dao!!.append(user))
        }

        for ((i, user) in this.dao!!.withIndex()) {
            assertEquals(users[i], user)
        }
    }
}