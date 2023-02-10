package dev.dres.data.model.media

interface MediaItem {

    val name: String
    val id: String?
    val collection: MediaItemCollection

    fun type(): MediaItemType
}