package dres.integration

import dev.dres.mgmt.MediaCollectionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

/**
 * Creating a collection with a taken name or path used to return `null` for every failure, so the
 * handler answered a generic 400 and genuine faults (an uninitialised store among them) looked identical
 * to a duplicate. The manager now throws and the message names the conflict.
 */
class MediaCollectionConflictTest : AbstractDresIntegrationTest() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initManagers() {
            MediaCollectionManager.init(store)
        }
    }

    @Test
    fun `creating a collection with a duplicate name is rejected with a named conflict`(@TempDir folder: Path) {
        val name = "conflict-${UUID.randomUUID()}"
        val created = MediaCollectionManager.createCollection(name, null, folder.toString())
        assertNotNull(created.id)

        val failure = assertThrows(IllegalArgumentException::class.java) {
            MediaCollectionManager.createCollection(name, null, "${folder}2")
        }
        assertTrue(
            failure.message!!.contains("already exists"),
            "The message must explain the conflict, but was: ${failure.message}"
        )
    }

    @Test
    fun `creating a collection with a duplicate path is rejected`(@TempDir folder: Path) {
        MediaCollectionManager.createCollection("first-${UUID.randomUUID()}", null, folder.toString())

        assertThrows(IllegalArgumentException::class.java) {
            MediaCollectionManager.createCollection("second-${UUID.randomUUID()}", null, folder.toString())
        }
    }

    /** A successful create must still hand back the entity, not null. */
    @Test
    fun `creating a collection returns it`(@TempDir folder: Path) {
        val created = MediaCollectionManager.createCollection("ok-${UUID.randomUUID()}", "d", folder.toString())

        assertEquals("d", created.description)
        assertTrue(created.itemCount == 0)
    }
}
