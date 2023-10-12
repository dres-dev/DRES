package dev.dres.api.rest.types.template.tasks

import dev.dres.api.rest.types.task.ApiContentElement

/**
 * Describes a [ApiHintContent] that should be displayed the user as hint for a task run.  This is a representation
 * used in the RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [ApiHintContent] was derived from.
 * @param sequence Sequence of [ApiContentElement]s to display.
 * @param loop Specifies if last [ApiContentElement] should be displayed until the end or if the entire sequence should be looped.
 */
data class ApiHintContent(val taskId: String, val sequence: List<ApiContentElement> = emptyList(), val loop: Boolean = false)
