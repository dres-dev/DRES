package dres.data.model.log

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class QueryEvent(val timestamp: Long = -1, val category: String = "", val type: List<String> = emptyList(), val value: String?)

data class QueryEventLog internal constructor(val timestamp: Long = -1,
                                              val events: List<QueryEvent> = emptyList(),
                                              val type: String = "",
                                              internal val serverTimeStamp: Long = System.currentTimeMillis())

data class QueryResult(val video: String = "", val shot: Int? = null, val frame: Int? = null, val score: Double? = null, val rank: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResultLog internal constructor(val timestamp: Long = -1,
                                               val values: List<String> = emptyList(), val usedCategories: List<String> = emptyList(),
                                               val usedTypes: List<String> = emptyList(), val sortType: List<String> = emptyList(),
                                               val resultSetAvailability: String = "", val results: List<QueryResult> = emptyList(),
                                               internal val serverTimeStamp: Long = System.currentTimeMillis())