package dev.dres.api.rest.types.submission

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

@JsonIgnoreProperties(ignoreUnknown = true)
data class RunResult(
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val tasks: List<TaskResult>,
    val timeStamp: Int = -1
) {
    @field:JsonIgnore
    @get:JsonIgnore
    internal val serverTimeStamp: Long = System.currentTimeMillis()
}
