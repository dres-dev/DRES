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

}