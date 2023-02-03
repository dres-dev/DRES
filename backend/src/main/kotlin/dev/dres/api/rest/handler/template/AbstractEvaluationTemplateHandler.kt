package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.TemplateId
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 * An abstract [RestHandler] used to access and manipulate [DbEvaluationTemplate]s.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 1.0.0
 */
abstract class AbstractEvaluationTemplateHandler(protected val store: TransientEntityStore) : RestHandler, AccessManagedRestHandler {

    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    /** All [AbstractCollectionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /** Convenience method to extract [DbEvaluationTemplate]'s ID from [Context]. */
    private fun competitionId(ctx: Context): TemplateId =
        ctx.pathParamMap().getOrElse("competitionId") {
            throw ErrorStatusException(404, "Parameter 'competitionId' is missing!'", ctx)
        }

    /** Convenience method to extract [DbEvaluationTemplate] from [Context]. */
    protected fun competitionFromContext(ctx: Context): DbEvaluationTemplate = competitionById(competitionId(ctx), ctx)

    /** Convenience method to extract [DbEvaluationTemplate] by ID. */
    protected fun competitionById(id: TemplateId, ctx: Context): DbEvaluationTemplate
        = DbEvaluationTemplate.query(DbEvaluationTemplate::id eq id).firstOrNull() ?: throw ErrorStatusException(404, "Competition with ID $id not found.'", ctx)
}