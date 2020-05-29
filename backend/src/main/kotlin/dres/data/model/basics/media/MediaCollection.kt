package dres.data.model.basics.media

import dres.data.model.Entity

data class MediaCollection(override var id: Long = -1, val name: String, val description: String?, val basePath: String) : Entity