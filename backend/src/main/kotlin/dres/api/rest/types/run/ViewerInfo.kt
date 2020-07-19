package dres.api.rest.types.run

/**
 * Basic information regarding a viewer instance
 *
 * @author Ralph Gasser
 * @version 1.1
 */
data class ViewerInfo(val viewersId: String, val username: String, val host: String, val ready: Boolean)