package dev.dres.data.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
        val httpPort: Int = 8080,
        val httpsPort: Int = 8443,
        val enableSsl: Boolean = true,
        val keystorePath: String = "keystore.jks",
        val keystorePassword: String = "password",
        val externalPath: String = "./external",
        val dataPath: String = "./data",
        val cachePath: String = "./cache",
        val statisticsPath: String = "./statistics",
        val eventsPath: String = "./events",
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
