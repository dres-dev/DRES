package dev.dres.data.dbo

import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.serializers.*
import java.nio.file.Path

/**
 * The data access layer used by DRES
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DataAccessLayer(private val basePath: Path) {
    /** List of [dev.dres.data.model.admin.User]s managed by this DRES instance. */
    val users = DAO(this.basePath.resolve("users.db"), UserSerializer, cacheDuration = 1440)

    val collections = DAO(this.basePath.resolve("collections.db"), MediaCollectionSerializer)
    val collectionNameIndex = DaoIndexer(collections){it.name}
    val collectionUidIndex = DaoIndexer(collections){it.id}

    val mediaItems = DAO(this.basePath.resolve("mediaItems.db"), MediaItemSerializer)
    val mediaItemCollectionIndex = DaoIndexer(mediaItems) { it.collection }
    val mediaItemCollectionNameIndex = DaoIndexer(mediaItems) { it.collection to it.name }
    val mediaItemCollectionUidIndex = DaoIndexer(mediaItems){it.collection to it.id}
    val mediaItemPathIndex = DaoIndexer(mediaItems){it.location}


    val mediaSegments = DAO(this.basePath.resolve("mediaSegments.db"), MediaItemSegmentSerializer, cacheSize = 10_000)
    val mediaSegmentItemIdIndex = DaoIndexer(mediaSegments){it.mediaItemId}

    private val competitionSerializer = CompetitionSerializer(mediaItems)

    /** List of [dev.dres.data.model.competition.CompetitionDescription]s managed by this DRES instance. */
    val competitions = DAO(this.basePath.resolve("competitions.db"), competitionSerializer)

    /** List of [dev.dres.data.model.run.InteractiveSynchronousCompetition]s managed by this DRES instance. */
    val runs: DAO<Competition> = DAO(this.basePath.resolve("runs.db"), CompetitionRunSerializer(competitionSerializer))

    val audit = DAO(this.basePath.resolve("auditLog.db"), AuditLogEntrySerializer, cacheDuration = 1440)
    val auditTimes = NumericDaoIndexer(audit){it.timestamp}
}