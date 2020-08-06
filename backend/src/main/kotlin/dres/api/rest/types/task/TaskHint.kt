package dres.api.rest.types.task

/**
 * Describes a [TaskHint] that should be displayed the user as hint for a task run.  This is a representation
 * used in the RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [TaskHint] was derived from.
 * @param sequence Sequence of [ContentElement]s to display.
 * @param loop Specifies if last [ContentElement] should be displayed until the end or if the entire sequence should be looped.
 */
data class TaskHint(val taskId: String, val sequence: List<ContentElement> = emptyList(), val loop: Boolean = false)
