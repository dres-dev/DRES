package dres.data.model.log

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


data class QueryEvent(val timestamp: Long, val category: String, val type: List<String>, val value: String?)

data class QueryEventLog internal constructor(val team: Int, val member: Long, val timestamp: Long, val events: List<QueryEvent>, val serverTimeStamp: Long, val type:String) {
    @JsonCreator
    constructor(@JsonProperty("teamId") team: Int, @JsonProperty("memberId") member: Long, @JsonProperty("timestamp") timestamp: Long, @JsonProperty("events") events: List<QueryEvent>, @JsonProperty("type" )type: String) : this(team, member, timestamp, events, System.currentTimeMillis(), type)
}

data class QueryResult(val video: String, val shot: Int? = null, val frame: Int? = null, val score: Double? = null, val rank: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryResultLog internal constructor(val team: Int, val member: Long, val timestamp: Long, val values: List<String>, val usedCategories: List<String>,
                                               val usedTypes: List<String>, val sortType: List<String>,
                                               val resultSetAvailability: String, val results: List<QueryResult>, val serverTimeStamp: Long){
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