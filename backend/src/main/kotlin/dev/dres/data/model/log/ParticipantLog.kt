package dev.dres.data.model.log

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswer
import io.javalin.openapi.OpenApiIgnore
import kotlinx.serialization.Serializable

enum class QueryEventCategory {
    TEXT, IMAGE, SKETCH, FILTER, BROWSING, COOPERATION, OTHER
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class QueryEvent(
    val timestamp: Long = -1,
    val category: QueryEventCategory = QueryEventCategory.OTHER,
    val type: String = "",
    val value: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class QueryEventLog internal constructor(
    val timestamp: Long = -1,
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val events: List<QueryEvent> = emptyList(),
    @field:JsonIgnore
    @get:JsonIgnore
    @get:OpenApiIgnore
    @kotlinx.serialization.Transient
    internal val serverTimeStamp: Long = System.currentTimeMillis()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class RankedAnswer(
    val answer: ApiClientAnswer,
    val rank: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class QueryResultLog internal constructor(
    val timestamp: Long = -1,
    val sortType: String = "",
    val resultSetAvailability: String = "",
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val results: List<RankedAnswer> = emptyList(),
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val events: List<QueryEvent> = emptyList(),
    @field:JsonIgnore
    @get:JsonIgnore
    @get:OpenApiIgnore
    @kotlinx.serialization.Transient
    internal val serverTimeStamp: Long = System.currentTimeMillis()
)