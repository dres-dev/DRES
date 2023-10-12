package dev.dres.data.migration

import dev.dres.data.model.config.Config
import jetbrains.exodus.database.TransientEntityStore

/**
 * General interface implemented by data migration scripts.
 *
 * TODO: Extend
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface Migration {
    /**
     * Performs data migration.
     *
     * @param config The global [Config] used for the data migration.
     * @param store The [TransientEntityStore] used for data access.
     */
    fun migrate(config: Config, store: TransientEntityStore)
}