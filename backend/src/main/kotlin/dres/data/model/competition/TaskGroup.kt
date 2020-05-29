package dres.data.model.competition

/**
 * A [TaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * TODO: Make validator & scorer for TaskGroup configurable.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
data class TaskGroup(val name: String, val type: TaskType, val defaultTaskDuration: Long) {}