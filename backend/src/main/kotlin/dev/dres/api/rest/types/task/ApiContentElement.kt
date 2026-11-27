package dev.dres.api.rest.types.task

/**
 * Describes a [ApiContentElement], i.e., a piece of content that should be displayed to the user.
 *
 * Exactly one of [content] and [mediaUrl] is used for non-empty content types:
 *  - [mediaUrl] points at a short-lived [dev.dres.api.rest.handler.preview.MediaGrant] that the
 *    requesting session may redeem. This is the preferred form for binary media because the payload
 *    can be fetched, cached, and prefetched by the browser, and because the grant can be scoped to the
 *    session and revoked when the task ends.
 *  - [content] still carries inline base64 for clients that cannot resolve a URL. It is only
 *    populated for content the caller is already entitled to and remains supported for
 *    backwards compatibility.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 *
 * @param contentType [ApiContentType] of the content held by this [ApiContentElement]
 * @param content Inline base64-encoded content, if any. Use [ApiContentType] to decode.
 * @param offset Time in seconds after which this [ApiContentElement] should be displayed.
 * @param mediaUrl Session-bound URL of the media payload, relative to the API root, if available.
 */
data class ApiContentElement(
    val contentType: ApiContentType,
    val content: String? = null,
    val offset: Long,
    val mediaUrl: String? = null
)
