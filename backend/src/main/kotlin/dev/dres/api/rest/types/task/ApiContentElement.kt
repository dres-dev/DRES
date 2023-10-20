package dev.dres.api.rest.types.task

/**
 * Describes a [ApiContentElement], i.e., a piece of content that should be displayed to the user.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
 *
 * @param content Encoded content; use [ApiContentType] to decode.
 * @param contentType [ApiContentType] of the content held by this [ApiContentElement]
 * @param offset Time in seconds after which this [ApiContentElement] should be displayed.
 */
data class ApiContentElement(val contentType: ApiContentType, val content: String? = null, val offset: Long)