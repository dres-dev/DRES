package dev.dres.data.model.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tests for sensible [Config] defaults and SSL configuration validation.
 *
 * The default deployment must start without a keystore: no keystore ships with the distribution,
 * so SSL is disabled by default and the backend must fail fast (with an actionable message) when
 * SSL is explicitly enabled but the configured keystore cannot be read.
 */
class ConfigTest {

    @Test
    fun `ssl is disabled by default`() {
        assertFalse(Config().enableSsl, "SSL must be disabled by default because no keystore ships with the distribution")
    }

    @TempDir
    lateinit var tempDir: Path

    private fun writeConfig(json: String): Path {
        val file = tempDir.resolve("config-${System.nanoTime()}.json")
        Files.writeString(file, json)
        return file
    }

    @Test
    fun `config without enableSsl key defaults to plain http`() {
        val config = Config.read(writeConfig("""{ "httpPort": 1234 }"""))
        assertFalse(config.enableSsl)
        assertEquals(1234, config.httpPort)
    }

    @Test
    fun `config with enableSsl key is honoured`() {
        val config = Config.read(writeConfig("""{ "enableSsl": true, "keystorePath": "custom.jks" }"""))
        assertTrue(config.enableSsl)
        assertEquals("custom.jks", config.keystorePath)
    }
}
