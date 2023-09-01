package dev.dres.data.model.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import dev.dres.DRES
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    /** HTTP Port */
    val httpPort: Int = 8080,
    /** HTTPS Port, only used when [enableSsl] is set to `true` */
    val httpsPort: Int = 8443,
    /** Whether to use secure connection or not. Requires keystore and keystore password */
    val enableSsl: Boolean = true,
    /** The path to the keystore file */
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
