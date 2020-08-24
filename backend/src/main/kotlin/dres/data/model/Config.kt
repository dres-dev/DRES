package dres.data.model


import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

data class Config(
        val httpPort: Int = 8080,
        val httpsPort: Int = 8443,
        val enableSsl: Boolean = true,
        val keystorePath: String = "keystore.jks",
        val keystorePassword: String = "password",
        val externalPath: String = "./external",
        val dataPath: String = "./data_branch",
        val cachePath: String = "./cache") {

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