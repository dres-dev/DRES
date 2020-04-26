package dres.data.model.log

import kotlinx.serialization.Serializable

@Serializable
data class QueryEvent(val timestamp: Long, val category: String, val type: String, val value: String)

@Serializable
data class QueryEventLog(val team: Int, val member: Long, val timestamp: Long, val events: List<QueryEvent>)

@Serializable
data class QueryResult(val video: String, val shot: Int, val score: Double? = null, val rank: Int? = null)

@Serializable
data class QueryResultLog(val team: Int, val member: Long, val timestamp: Long, val usedCategories: List<String> = emptyList(),
                          val usedTypes: List<String> = emptyList(), val sortType: List<String> = emptyList(),
                          val resultSetAvailability: String = "", val events: List<QueryResult>)