package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.*
import dev.dres.api.rest.handler.evaluation.client.AbstractEvaluationClientHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.EvaluationId
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.flatMapDistinct
import kotlinx.dnq.query.isEmpty
import kotlinx.dnq.query.isNotEmpty

/**
 * A [RestHandler] that provides basic functionality to query the state of [Evaluation]s. This is part of the internal DRES API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractEvaluationViewerHandler(protected val store: TransientEntityStore): RestHandler, AccessManagedRestHandler {
    /** By default [AbstractEvaluationViewerHandler] can only be used by [ApiRole.VIEWER]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.VIEWER)

    /** All [AbstractEvaluationClientHandler]s are part of the v1 API. */
    override val apiVersion = "v1"

    /**
     * Returns the [RunManager] associated with the current [Context].
     *
     * @param ctx The request [Context]
     * @return [RunManager] or null
     */
    fun getEvaluationManager(ctx: Context, evaluationId: EvaluationId): InteractiveRunManager? {
        val run = RunExecutor.managerForId(evaluationId) ?: return null
        if (run !is InteractiveRunManager) return null
        if (ctx.isParticipant() && run.template.teams.flatMapDistinct { it.users }.filter { it.id eq ctx.userId() }.isEmpty) return null
        return run
    }

    /**
     * Returns the [RunManager] associated with the current [Context].
     *
     * @param ctx The request [Context]
     * @return [RunManager] or null
     */
    fun getRelevantManagers(ctx: Context): List<InteractiveRunManager> {
        val managers = RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java)
        return when {
            ctx.isParticipant() -> managers.filter { m -> m.template.teams.flatMapDistinct { it.users }.filter { it.id eq ctx.userId() }.isNotEmpty }
            ctx.isJudge() -> managers.filter { m -> m.template.judges.filter { u -> u.id eq ctx.userId() }.isNotEmpty }
            else -> managers
        }
    }
}