package dev.dres.data.model.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    val httpPort: Int = 8080,
    val httpsPort: Int = 8443,
    val enableSsl: Boolean = true,
    val keystorePath: String = "keystore.jks",
    val keystorePassword: String = "password",
    val cache: CacheConfig= CacheConfig()
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