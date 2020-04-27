package dres.data.model.basics.media

import dres.data.model.Entity
import kotlinx.serialization.Serializable

@Serializable
data class MediaCollection(override var id: Long = -1, val name: String, val description: String?, val basePath: String) : Entity