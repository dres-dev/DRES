package dev.dres.data.dbo

import dev.dres.data.model.admin.PlainPassword
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserName
import dev.dres.data.serializers.UserSerializer
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
//        val user1 = User(username = UserName("rgasser"), password = PlainPassword("12345").hash(), role = Role.ADMIN)
//        val user2 = User(username = UserName("lrossetto"), password = PlainPassword("45678").hash(), role = Role.ADMIN)
//        val user3 = User(username = UserName("vbs2021"), password = PlainPassword("abcde").hash(), role = Role.JUDGE)
//
//        assertEquals(1L, this.dao!!.append(user1))
//        assertEquals(2L, this.dao!!.append(user2))
//        assertEquals(3L, this.dao!!.append(user3))
//
//
//        assertEquals(user1, this.dao!![1L])
//        assertEquals(user2, this.dao!![2L])
//        assertEquals(user3, this.dao!![3L])
    }

    /**
     * Tests the [DAO]s append method.
     */
    @Test
    fun testIteration() {
        val users = arrayOf(
                User(username = UserName("rgasser"), password = PlainPassword("12345").hash(), role = Role.ADMIN),
                User(username = UserName("lrossetto"), password = PlainPassword("45678").hash(), role = Role.ADMIN),
                User(username = UserName("vbs2021"), password = PlainPassword("abcde").hash(), role = Role.JUDGE),
                User(username = UserName("testUser1"), password = PlainPassword("lalala").hash(), role = Role.VIEWER),
                User(username = UserName("testUser2"), password = PlainPassword("truestory!").hash(), role = Role.VIEWER)
        )


        for ((i, user) in users.withIndex()) {
            assertEquals(i+1L, this.dao!!.append(user))
        }

        for ((i, user) in this.dao!!.withIndex()) {
            assertEquals(users[i], user)
        }
    }
}