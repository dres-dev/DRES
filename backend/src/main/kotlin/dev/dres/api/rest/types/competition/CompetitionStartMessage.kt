package dev.dres.api.rest.types.competition

import dev.dres.api.rest.types.run.RunType
import dev.dres.data.model.UID
import dev.dres.data.model.run.RunProperties
import dev.dres.utilities.extensions.UID

/**
 * A data class that represents a RESTful request for creating a new [dres.data.model.run.CompetitionRun]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class CompetitionStartMessage(val competitionId: String, val name: String, val type: RunType, val properties: RunProperties = RunProperties())