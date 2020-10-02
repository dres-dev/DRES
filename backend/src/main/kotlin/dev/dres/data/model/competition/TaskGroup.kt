package dev.dres.data.model.competition

/**
 * A [TaskGroup] allows the user to specify common traits among a group of [Task]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
data class TaskGroup constructor(val name: String, val type: String)