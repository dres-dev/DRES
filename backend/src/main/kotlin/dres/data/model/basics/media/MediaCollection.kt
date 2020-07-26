package dres.data.model.basics.media

import dres.data.model.Entity
import dres.data.model.UID
import java.util.*

data class MediaCollection(override var id: UID = UID.EMPTY, val name: String, val description: String?, val basePath: String) : Entity