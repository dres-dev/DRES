package dev.dres.data.model.media

interface MediaItem { //TODO


    val name: String
    val id: String?
    val type: MediaItemType
    val collection: MediaItemCollection

}