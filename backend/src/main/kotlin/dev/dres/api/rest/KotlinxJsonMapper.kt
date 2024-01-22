package dev.dres.api.rest


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.json.JsonMapper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.Type

object KotlinxJsonMapper : JsonMapper {

    private val fallbackMapper = jacksonObjectMapper()
    private val mapper = Json{
        ignoreUnknownKeys = true
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T {

        return try {
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(targetType) as KSerializer<T>
            mapper.decodeFromString(serializer, json)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalStateException) {
            null
        } ?: fallbackMapper.readValue(json, fallbackMapper.typeFactory.constructType(targetType))

    }

    override fun toJsonString(obj: Any, type: Type): String {

        return try {
            val serializer = serializer(type)
            mapper.encodeToString(serializer, obj)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalStateException) {
            null
        } ?: fallbackMapper.writeValueAsString(obj)

    }
}