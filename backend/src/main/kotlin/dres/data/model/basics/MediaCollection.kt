package dres.data.model.basics

import dres.data.model.Entity

data class MediaCollection(override var id: Long = -1, val name: String, val description: String?) : Entity