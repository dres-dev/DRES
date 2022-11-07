package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.Evaluation
import dev.dres.run.score.scoreboard.ScoreOverview
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 * A collection of [RestHandler]s that deal with [ScoreOverview]s for ongoing [Evaluation]s.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
abstract class AbstractScoreHandler(protected val store: TransientEntityStore) : RestHandler, AccessManagedRestHandler {
    /** By default [AbstractScoreHandler] can only be used by [ApiRole.VIEWER]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.VIEWER)

    /** All [AbstractScoreHandler]s are part of the v1 API. */
    override val apiVersion = "v1"
}