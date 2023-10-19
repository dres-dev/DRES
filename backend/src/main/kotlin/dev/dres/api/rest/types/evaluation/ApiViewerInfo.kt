package dev.dres.api.rest.types.evaluation

/**
 * Basic information regarding a viewer instance
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiViewerInfo(val viewersId: String, val username: String, val host: String, val ready: Boolean)