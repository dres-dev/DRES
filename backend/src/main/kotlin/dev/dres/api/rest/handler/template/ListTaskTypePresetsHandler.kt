package dev.dres.api.rest.handler.template

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import dev.dres.DRES
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.api.rest.types.users.ApiRole
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.security.RouteRole
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.streams.toList

/**
 * A [GetRestHandler] that can be used to list [ApiTaskType] presets stored on disk.
 * There are two locations that are checked:
 * 1. The "shipped with DRES" _internal_ presets (`src/main/resources/dres-type-presets`)
 * 2. Custom _external_ presets (`$DATA_FOLDER/type-presets`)
 *
 * Both lists are lexicographically sorted and in case two presets have the same name, the external
 * one overrides the internal preset. This way instance-specific defaults can be set.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
class ListTaskTypePresetsHandler : AccessManagedRestHandler, GetRestHandler<List<ApiTaskType>> {
    override val route: String = "template/type-presets/list"
    override val apiVersion: String = "v2"

    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    @OpenApi(
        summary = "Lists the task type presets available. Both, shipped with DRES and custom ones.",
        path = "/api/v2/template/type-presets/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiTaskType>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiTaskType> {
        /** Internal presets first */
        val loader = this::class.java.classLoader
        val uri = loader.getResource(DRES.TASK_TYPE_PRESETS_LOCATION).toURI()
        LOGGER.debug("Found presets location: {}", uri)
        val internalPresets = if ("jar" == uri.scheme) {
            /** JAR Handling */
            try{
                val list = FileSystems.newFileSystem(uri, emptyMap<String, String>(), loader).use { fs ->
                    val files = Files.walk(fs.getPath("/${DRES.TASK_TYPE_PRESETS_LOCATION}"))
                        .filter { it.isReadable() and (it.extension == "json") }.toList()
                    LOGGER.trace("Internal preset files (jar): {}", files.toList().toString())
                    return@use files.sortedBy { it.fileName }
                }
            val list2 = list.map{this.javaClass.getResource(it.toString())}

            LOGGER.trace("Internal preset files (jar) resources: {}", list2.toString())

            list2.map{ ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false).readValue(it, ApiTaskType::class.java) }
            }catch(e: Exception){
                LOGGER.error("Error upon loading internal presets.", e)
                LOGGER.warn("An error occurred during loading of internal task type presets. Only external ones are available. Check logs accordingly.")
                emptyList<ApiTaskType>()
            }
        } else {
            /** IDE handling */
            val files = Paths.get(uri).listDirectoryEntries("*.json").filter { it.isReadable() }.sortedBy { it.fileName }
            LOGGER.trace("Internal preset files (dir): {}", files.toString())
            files.map { ApiTaskType.read(it) }
        }
        LOGGER.trace("Found {} internal presets.", internalPresets.size)

        Files.createDirectories(DRES.TASK_TYPE_PRESETS_EXTERNAL_LOCATION)

        /** External presets second */
        val externalPresets =
            DRES.TASK_TYPE_PRESETS_EXTERNAL_LOCATION.listDirectoryEntries("*.json").sortedBy { it.fileName }.map {
                ApiTaskType.read(it)
            }
        LOGGER.trace("External presets: {}", externalPresets.toString())
        LOGGER.trace("Found {} external presets", externalPresets.size)

        /** Merge such that external presets may override internal ones */
        val presets = mutableListOf<ApiTaskType>()
        presets.addAll(externalPresets)
        presets.addAll(0,internalPresets.filter {
            !externalPresets.map { it.name }.contains(it.name)
        })
        return presets
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}
