package dev.dres.data.model.log

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryEvent(val timestamp: Long = -1, val category: String = "", @field:JsonSetter(contentNulls = Nulls.FAIL) val type: List<String> = emptyList(), val value: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryEventLog internal constructor(val timestamp: Long = -1,
                                              @field:JsonSetter(contentNulls = Nulls.FAIL) val events: List<QueryEvent> = emptyList(),
                                              val type: String = "",
                                              internal val serverTimeStamp: Long = System.currentTimeMillis())
@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResult(val video: String = "", val shot: Int? = null, val frame: Int? = null, val score: Double? = null, val rank: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResultLog internal constructor(val timestamp: Long = -1,
                                               @field:JsonSetter(contentNulls = Nulls.FAIL) val values: List<String> = emptyList(),
                                               @field:JsonSetter(contentNulls = Nulls.FAIL) val usedCategories: List<String> = emptyList(),
                                               @field:JsonSetter(contentNulls = Nulls.FAIL) val usedTypes: List<String> = emptyList(),
                                               @field:JsonSetter(contentNulls = Nulls.FAIL) val sortType: List<String> = emptyList(),
                                               val resultSetAvailability: String = "",
                                               @field:JsonSetter(contentNulls = Nulls.FAIL)val results: List<QueryResult> = emptyList(),
                                               internal val serverTimeStamp: Long = System.currentTimeMillis())