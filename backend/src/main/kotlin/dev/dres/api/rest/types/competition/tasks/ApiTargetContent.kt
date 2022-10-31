package dev.dres.api.rest.types.competition.tasks

import dev.dres.api.rest.types.task.ApiContentElement

/**
 * Describes a [ApiTargetContent] that should be displayed the user after the task run has finished. This is a representation
 * used in the RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [ApiTargetContent] was derived from.
 * @param sequence Sequence of [ApiContentElement]s to display.
 */
data class ApiTargetContent(val taskId: String, val sequence: List<ApiContentElement> = emptyList())
