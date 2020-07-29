package dres.api.rest.types.query

/**
 * Describes a [QueryContentElement], i.e., a piece of content that should be displayed to the user.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param content Encoded content; use [ContentType] to decode.
 * @param contentType [ContentType] of the content held by this [QueryContentElement]
 * @param offset Time in seconds after which this [QueryContentElement] element should become active.
 * @param duration Duration in seconds for which this [QueryContentElement] element should be displayed.
 */
data class QueryContentElement(val content: String, val contentType: ContentType, val offset: Int = 0, val duration: Int? = null)
