package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaType

enum class MediaItemType {
    IMAGE, VIDEO, TEXT;

    fun toApi() = when(this) {
        IMAGE -> ApiMediaType.IMAGE
        VIDEO -> ApiMediaType.VIDEO
        TEXT -> ApiMediaType.TEXT
    }

    fun toDb() = when(this) {
        IMAGE -> DbMediaType.IMAGE
        VIDEO -> DbMediaType.VIDEO
        TEXT -> DbMediaType.TEXT
    }

    companion object {

        fun fromApi(type: ApiMediaType) = when(type) {
            ApiMediaType.IMAGE -> IMAGE
            ApiMediaType.VIDEO -> VIDEO
            ApiMediaType.TEXT -> TEXT
        }

        fun fromDb(type: DbMediaType) = when(type) {
            DbMediaType.IMAGE -> IMAGE
            DbMediaType.VIDEO -> VIDEO
            DbMediaType.TEXT -> TEXT
            else -> throw IllegalStateException("Unknown DbMediaType $type")
        }

    }
}