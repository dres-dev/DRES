package dres.api.rest.types.query

/**
 * Describes a [QueryTarget] that should be displayed the user after the task run has finished. This is a representation
 * used in  he RESTful API to transfer information regarding the query. Always derived from a [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param taskId of the [TaskDescription] this [QueryTarget] was derived from.
 * @param sequence Sequence of [QueryContentElement]s to display.
 */
data class QueryTarget(val taskId: String, val sequence: List<QueryContentElement> = emptyList())
