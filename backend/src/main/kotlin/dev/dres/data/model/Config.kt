package dev.dres.data.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    /**
     * The port to use for the server (when ssl is off)
     */
        val httpPort: Int = 8080,
    /**
     * The port to use for the server (when ssl is on)
     */
        val httpsPort: Int = 8443,
    /**
     * Flag whether or not to use SSL
     */
        val enableSsl: Boolean = true,
    /**
     * The path (relative or absolute) to the keystore file (for SSL)
     */
        val keystorePath: String = "keystore.jks",
    /**
     * The keystore password in plaintext. Do not commit this.
     */
        val keystorePassword: String = "password",
    /**
     * The path to external files (i.e. for external query hints)
     */
        val externalPath: String = "./external",
    /**
     * The path to the data files (i.e. the database)
     */
        val dataPath: String = "./data",
    /**
     * The path to the cache files
     */
        val cachePath: String = "./cache",
    /**
     * The path to the statistics files. DRES will write statistics to this location.
     */
        val statisticsPath: String = "./statistics",
    /**
     * The path to the event files. DRES will write events to this location.
     */
        val eventsPath: String = "./events",
        // TODO add more paths (audit log)
    /**
     * The max size of logos
     */
        val logoMaxSize: Int = 1000) {

    companion object{
        fun read(file: File): Config? {
            val mapper = ObjectMapper()
            return try {
                mapper.readValue(file, Config::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
