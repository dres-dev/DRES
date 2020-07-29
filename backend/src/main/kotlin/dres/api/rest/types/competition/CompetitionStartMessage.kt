package dres.api.rest.types.competition

import dres.api.rest.types.run.RunType
import dres.data.model.UID
import dres.utilities.extensions.UID

/**
 * A data class that represents a RESTful request for creating a new [dres.data.model.run.CompetitionRun]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class CompetitionStartMessage(val competitionId: String, val name: String, val type: RunType, val scoreboards: Array<String>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompetitionStartMessage

        if (competitionId != other.competitionId) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (!scoreboards.contentEquals(other.scoreboards)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = competitionId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + scoreboards.contentHashCode()
        return result
    }
}