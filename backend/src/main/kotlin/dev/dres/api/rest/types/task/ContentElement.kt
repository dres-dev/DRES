package dev.dres.api.rest.types.task

/**
 * Describes a [ContentElement], i.e., a piece of content that should be displayed to the user.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 * @param content Encoded content; use [ContentType] to decode.
 * @param contentType [ContentType] of the content held by this [ContentElement]
 * @param offset Time in seconds after which this [ContentElement] element should become active.
 */
data class ContentElement(val contentType: ContentType, val content: String? = null, val offset: Long = 0)