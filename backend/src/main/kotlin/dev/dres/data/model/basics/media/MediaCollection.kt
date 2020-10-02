package dev.dres.data.model.basics.media

import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import java.util.*

data class MediaCollection(override var id: UID = UID.EMPTY, val name: String, val description: String?, val basePath: String) : Entity