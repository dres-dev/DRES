package dres.data.model.competition

data class QueryDescription (val taskName: String, val query: QueryContent, val reveal: QueryContent? = null)

/**
 * @param content encoded content, base64 in case of non-textural media
 * @param contentType mime tyoe of the [content]
 * @param offset time in seconds after which this content element becomes active
 * @param maxDuration maximum continuous time in seconds which this content element is to displayed
 */
data class QueryContentElement (val content: String, val contentType: String, val offset: Int = 0, val maxDuration: Int? = null)

/**
 * @param loop specifies if display should stay with the last element or loop the entire sequence
 */
data class QueryContent (val text: List<QueryContentElement> = emptyList(),
                         val image: List<QueryContentElement> = emptyList(),
                         val audio: List<QueryContentElement> = emptyList(),
                         val video: List<QueryContentElement> = emptyList(),
                         val loop: Boolean = false)

