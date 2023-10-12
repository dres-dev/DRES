package dev.dres.data.model.media

typealias MediaItemId = String

interface MediaItem {

    /** */
    val name: String

    /** */
    val mediaItemId: MediaItemId

    /** */
    fun dbCollection(): MediaItemCollection

    /** */
    fun type(): MediaItemType
}