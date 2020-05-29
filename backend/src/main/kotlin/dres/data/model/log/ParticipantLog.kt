package dres.data.model.log


data class QueryEvent(val timestamp: Long, val category: String, val type: String, val value: String)

data class QueryEventLog internal constructor(val team: Int, val member: Long, val timestamp: Long, val events: List<QueryEvent>, val serverTimeStamp: Long) {
    constructor(team: Int, member: Long, timestamp: Long, events: List<QueryEvent>) : this(team, member, timestamp, events, System.currentTimeMillis())
}

data class QueryResult(val video: String, val shot: Int, val score: Double? = null, val rank: Int? = null)

data class QueryResultLog internal constructor(val team: Int, val member: Long, val timestamp: Long, val usedCategories: List<String>,
                          val usedTypes: List<String>, val sortType: List<String>,
                          val resultSetAvailability: String, val events: List<QueryResult>, val serverTimeStamp: Long){
    constructor(team: Int, member: Long, timestamp: Long, usedCategories: List<String> = emptyList(),
                usedTypes: List<String> = emptyList(), sortType: List<String> = emptyList(),
                resultSetAvailability: String = "", events: List<QueryResult>) : this (team, member, timestamp, usedCategories, usedTypes, sortType, resultSetAvailability, events, System.currentTimeMillis())
}