package dev.dres.run.exceptions

import dev.dres.data.model.template.team.TeamId
import dev.dres.run.RunManager

/**
 * An [IllegalStateException] that gets thrown whenever a [RunManager] or a dependent class does not know a [TeamId] it is supposed to process.
 *
 * Errors like this are usually linked to bad user input.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class IllegalTeamIdException(val teamId: TeamId) : IllegalStateException("Could not execute request because run manager does not know the given team ID $teamId.")