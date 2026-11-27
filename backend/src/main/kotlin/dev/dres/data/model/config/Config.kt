package dev.dres.data.model.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import dev.dres.DRES
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Additional configuration section for cross-origin access.
 *
 * @property allowedOrigins Hosts (optionally with port) that may send credentialed cross-origin requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CorsConfig(
    /** Origins allowed to send credentialed requests; empty means "none". */
    val allowedOrigins: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    /** HTTP Port */
    val httpPort: Int = 8080,
    /** HTTPS Port, only used when [enableSsl] is set to `true` */
    val httpsPort: Int = 8443,
    /** Whether to serve HTTPS in addition to HTTP. Requires a readable keystore at [keystorePath]; the backend refuses to start if SSL is enabled without one. Disabled by default. */
    val enableSsl: Boolean = false,
    /** The path to the keystore file, resolved relative to the working directory. Only used when [enableSsl] is set to `true` */
    val keystorePath: String = "keystore.jks",
    /** Keystore password */
    val keystorePassword: String = "password",
    /** The [CacheConfig] */
    val cache: CacheConfig = CacheConfig(),
    /** Folder that contains the database. Defaults to `$APPLICATION_ROOT/../data`*/
    val dataPath: Path = DRES.APPLICATION_ROOT.parent.resolve("data"),
    /** Folder that contains external media. Defaults to `$cwd/external` */
    val externalMediaLocation : Path = Paths.get("./external"),
    /** Folder that contains task type rpesets. Defaults to `$cwd/type-presets` */
    val presetsLocation: Path = Paths.get("./type-presets"),
    /** Folder for event data. Defaults to `$APPLICATION_ROOT/../events` */
    val eventsLocation: Path = DRES.APPLICATION_ROOT.parent.resolve("events"),
    /** Location for Audit event log. Defaults to `$APPLICATION_ROOT/../audit`*/
    val auditLocation: Path = DRES.APPLICATION_ROOT.parent.resolve("audit"),
    /** Whether API endpoints for external media should be available */
    val externalMediaEndpointsEnabled: Boolean = false,
    /** Whether to enable http3 protocol, requires enableSsl to be true, to have an effect. THIS IS CURRENTLY NOT YET SUPPORTED */
    val enableHttp3: Boolean = false,
    /** Cross-origin policy for credentialed API access. */
    val cors: CorsConfig = CorsConfig(),
) {

    companion object{

        /**
         * Reads a configuration file from the specified [Path]
         *
         * @param path The [Path]
         */
        fun read(file: Path): Config = Files.newInputStream(file, StandardOpenOption.READ).use {
            ObjectMapper().readValue(it, Config::class.java)
        }
    }

}
