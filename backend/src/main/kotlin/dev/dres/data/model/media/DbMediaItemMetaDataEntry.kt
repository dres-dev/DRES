package dev.dres.data.model.media

import dev.dres.api.rest.types.collection.ApiMediaItemMetaDataEntry
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.xdParent
import kotlinx.dnq.xdRequiredStringProp

internal class DbMediaItemMetaDataEntry(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbMediaItemMetaDataEntry>()

    /** The key for this [DbMediaItemMetaDataEntry]. */
    var key by xdRequiredStringProp()

    /** The value for this [DbMediaItemMetaDataEntry]. */
    var value by xdRequiredStringProp()

    /** The [DbMediaItem] this [DbMediaItemMetaDataEntry] belongs to. */
    val item: DbMediaItem by xdParent<DbMediaItemMetaDataEntry, DbMediaItem>(DbMediaItem::metadata)

    fun asPair() = key to value

    fun toApi() = ApiMediaItemMetaDataEntry(key, value)

}