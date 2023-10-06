package dev.dres.api.rest.types.template.tasks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import dev.dres.api.rest.types.template.tasks.options.*
import dev.dres.data.model.template.task.DbTaskType
import io.javalin.openapi.OpenApiIgnore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

typealias TaskTypeId = String

/**
 * The RESTful API equivalent of a [DbTaskType].
 *
 * @author Ralph Gasser & Loris Sauter
 * @version 1.1.0
 */
data class ApiTaskType(
    val id: TaskTypeId? = null,
    val name: String,
    val duration: Long,
    val targetOption: ApiTargetOption,
    val hintOptions: List<ApiHintOption>,
    val submissionOptions: List<ApiSubmissionOption>,
    val taskOptions: List<ApiTaskOption>,
    val scoreOption: ApiScoreOption,
    val configuration: Map<String, String>
) {

    constructor() : this("---NO_ID---","---Default TaskType DO NOT USE!---",
    1,
        ApiTargetOption.TEXT, listOf(ApiHintOption.TEXT),
        listOf(ApiSubmissionOption.TEXTUAL_SUBMISSION),
        listOf(ApiTaskOption.HIDDEN_RESULTS), ApiScoreOption.KIS, mapOf()
    )

    companion object {
        /**
         * Reads an [ApiTaskType] from the given path
         *
         * @param file The path to read from
         */
        fun read(file: Path): ApiTaskType =
            Files.newInputStream(file, StandardOpenOption.READ).use {
                ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(it, ApiTaskType::class.java)
            }
    }

    val taskTypeId: TaskTypeId
        @JsonIgnore
        @OpenApiIgnore
        get() = this.id ?: "N/A"
}
