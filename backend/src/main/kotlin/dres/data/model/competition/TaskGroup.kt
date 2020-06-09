package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A [TaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * TODO: Make validator & scorer for TaskGroup configurable.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
data class TaskGroup @JsonCreator constructor(
        @JsonProperty("name") val name: String,
        @JsonProperty("type") val type: TaskType,
        @JsonProperty("defaultTaskDuration") val defaultTaskDuration: Long) {}