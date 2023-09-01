package dev.dres.api.rest.handler.template

import dev.dres.api.rest.RestApi
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
import kotlinx.dnq.query.and
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
abstract class AbstractEvaluationTemplateHandler() : RestHandler, AccessManagedRestHandler {

    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    /** All [AbstractCollectionHandler]s are part of the v2 API. */
    override val apiVersion = RestApi.LATEST_API_VERSION

    /** Convenience method to extract [DbEvaluationTemplate]'s ID from [Context]. */
    protected fun templateIdFromContext(ctx: Context): TemplateId =
        ctx.pathParamMap().getOrElse("templateId") {
            throw ErrorStatusException(404, "Parameter 'templateId' is missing!'", ctx)
        }

//    /** Convenience method to extract [DbEvaluationTemplate] from [Context]. */
//    protected fun evaluationTemplateFromContext(ctx: Context): DbEvaluationTemplate
//        = evaluationTemplateById(templateIdFromContext(ctx), ctx)
//
//    /** Convenience method to extract [DbEvaluationTemplate] by ID. */
//    protected fun evaluationTemplateById(id: TemplateId, ctx: Context): DbEvaluationTemplate
//        = DbEvaluationTemplate.query((DbEvaluationTemplate::id) eq id and (DbEvaluationTemplate::instance eq false)).firstOrNull() ?: throw ErrorStatusException(404, "Evaluation template with ID $id not found.'", ctx)
}
