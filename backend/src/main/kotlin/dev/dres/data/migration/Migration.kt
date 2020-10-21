package dev.dres.data.migration

import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config

/**
 * General interface implemented by data migration scripts.
 *
 * TODO: Extend
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface Migration {
    /**
     * Performs data migration.
     *
     * @param config The global [Config] used for the data migration.
     * @param data The [DataAccessLayer] used for data access.
     */
    fun migrate(config: Config, data: DataAccessLayer)
}