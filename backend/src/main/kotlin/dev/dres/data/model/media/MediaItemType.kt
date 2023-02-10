package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaType

interface MediaItemType {
    fun toApi(): ApiMediaType
}