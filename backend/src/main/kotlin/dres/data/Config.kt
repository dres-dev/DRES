package dres.data


import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class Config(val port: Int = 8080, val dataPath: String = "./data") {

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