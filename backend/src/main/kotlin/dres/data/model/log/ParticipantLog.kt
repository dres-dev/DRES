package dres.data.model.log

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


data class QueryEvent(val timestamp: Long = -1, val category: String = "", val type: List<String> = emptyList(), val value: String?)

data class QueryEventLog internal constructor(val team: Int = -1, val member: Long = -1,
                                              val timestamp: Long = -1,
                                              val events: List<QueryEvent> = emptyList(),
                                              val serverTimeStamp: Long = System.currentTimeMillis(),
                                              val type: String = "") {
    @JsonCreator
    constructor(
            @JsonProperty("teamId") team: Int = -1,
            @JsonProperty("memberId") member: Long = -1,
            @JsonProperty("timestamp") timestamp: Long = -1,
            @JsonProperty("events") events: List<QueryEvent> = emptyList(),
            @JsonProperty("type" )type: String = ""
    ) : this(team, member, timestamp, events, System.currentTimeMillis(), type)
}

data class QueryResult(val video: String = "", val shot: Int? = null, val frame: Int? = null, val score: Double? = null, val rank: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResultLog internal constructor(val team: Int = -1, val member: Long = -1, val timestamp: Long = -1,
                                               val values: List<String> = emptyList(), val usedCategories: List<String> = emptyList(),
                                               val usedTypes: List<String> = emptyList(), val sortType: List<String> = emptyList(),
                                               val resultSetAvailability: String = "", val results: List<QueryResult> = emptyList(),
                                               val serverTimeStamp: Long = System.currentTimeMillis()){
    @JsonCreator constructor(
            @JsonProperty("teamId") team: Int,
            @JsonProperty("memberId") member: Long,
            @JsonProperty("timestamp") timestamp: Long,
            @JsonProperty("values") values: List<String>,
            @JsonProperty("usedCategories") usedCategories: List<String> = emptyList(),
            @JsonProperty("usedTypes") usedTypes: List<String> = emptyList(),
            @JsonProperty("sortType") sortType: List<String> = emptyList(),
            @JsonProperty("resultSetAvailability") resultSetAvailability: String = "",
            @JsonProperty("results") results: List<QueryResult> = emptyList()) : this (team, member, timestamp, values, usedCategories, usedTypes, sortType, resultSetAvailability, results, System.currentTimeMillis())

}