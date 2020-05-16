package dres.data.model


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Serializable
data class Config(
        val httpPort: Int = 8080,
        val httpsPort: Int = 8443,
        val keystorePath: String = "keystore.jks",
        val keystorePassword: String = "password",
        val dataPath: String = "./data",
        val cachePath: String = "./cache") {

    companion object{
        fun read(file: File): Config? {
            val json = Json(JsonConfiguration.Stable)
            return try {
                json.parse(serializer(), file.readText())
            } catch (e: Exception) {
                null
            }
        }
    }
}