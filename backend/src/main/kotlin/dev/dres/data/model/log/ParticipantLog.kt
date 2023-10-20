package dev.dres.data.model.log

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.javalin.openapi.OpenApiIgnore

enum class QueryEventCategory {
    TEXT, IMAGE, SKETCH, FILTER, BROWSING, COOPERATION, OTHER
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryEvent(
    val timestamp: Long = -1,
    val category: QueryEventCategory = QueryEventCategory.OTHER,
    val type: String = "",
    val value: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryEventLog internal constructor(
    val timestamp: Long = -1,
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val events: List<QueryEvent> = emptyList(),
    @field:JsonIgnore
    @get:JsonIgnore
    @get:OpenApiIgnore
    internal val serverTimeStamp: Long = System.currentTimeMillis()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResult(
    val item: String = "",
    val segment: Int? = null,
    val frame: Int? = null,
    val score: Double? = null,
    val rank: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResultLog internal constructor(
    val timestamp: Long = -1,
    val sortType: String = "",
    val resultSetAvailability: String = "",
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val results: List<QueryResult> = emptyList(),
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val events: List<QueryEvent> = emptyList(),
    @field:JsonIgnore
    @get:JsonIgnore
    @get:OpenApiIgnore
    internal val serverTimeStamp: Long = System.currentTimeMillis()
)