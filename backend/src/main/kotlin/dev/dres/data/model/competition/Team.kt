package dev.dres.data.model.competition

import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.admin.UserId
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Represents a [Team] that takes part in a competition managed by DRES.
 *
 * @author Ralph Gasser, Loris Sauter, Luca Rossetto
 * @version 1.2.0
 */
data class Team constructor(val uid: TeamId, val name: String, val color: String, val logoId: UID, val users: MutableList<UserId>) {
    companion object {
        /**
         * Generates and returns the [Path] to the team logo with the given [logoId].
         *
         * @param logoId The ID of the desired logo.
         * @param config The global [Config] used to construct the [Path].
         */
        fun logoPath(config: Config, logoId: UID) = Paths.get(config.cachePath, "logos", "${logoId.string}.png")
    }
}