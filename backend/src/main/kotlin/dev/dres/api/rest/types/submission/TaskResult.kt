package dev.dres.api.rest.types.submission

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskResult(
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val task: String,                   /** the name of the task */
    val resultName: String = "default", /** optional name of a result set */
    val resultType: String? = null,     /** optional type information of a result set*/
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val results: List<ResultElement>    /** list of actual results*/
)
