package dres.api.rest.types.query

/**
 * Describes a [QueryHint] that should be displayed the user as hint for a task run.  This is a representation
 * used in the RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [QueryHint] was derived from.
 * @param sequence Sequence of [QueryContentElement]s to display.
 * @param loop Specifies if last [QueryContentElement] should be displayed until the end or if the entire sequence should be looped.
 */
data class QueryHint(val taskId: String, val sequence: List<QueryContentElement> = emptyList(), val loop: Boolean = false)
