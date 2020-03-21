package dres.data.dbo

import dres.data.dbo.DAO
import dres.data.serializers.CompetitionSerializer
import dres.data.serializers.MediaItemSerializer
import dres.data.serializers.UserSerializer
import java.nio.file.Path

/**
 * The data access layer used by DRES
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DataAccessLayer(private val basePath: Path) {
    /** List of [dres.data.model.admin.User]s managed by this DRES instance. */
    val users = DAO(this.basePath.resolve("users.db"), UserSerializer)

    /** List of [dres.data.model.competition.Competition]s managed by this DRES instance. */
    val competitions = DAO(this.basePath.resolve("competitions.db"), CompetitionSerializer)

    /** List of [dres.data.model.basics.MediaItem]s managed by this DRES instance. */
    val collection = DAO(this.basePath.resolve("collection.db"), MediaItemSerializer)
}