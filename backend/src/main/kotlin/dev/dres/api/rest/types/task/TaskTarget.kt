package dev.dres.api.rest.types.task

/**
 * Describes a [TaskTarget] that should be displayed the user after the task run has finished. This is a representation
 * used in  he RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [TaskTarget] was derived from.
 * @param sequence Sequence of [ContentElement]s to display.
 */
data class TaskTarget(val taskId: String, val sequence: List<ContentElement> = emptyList())
