package dev.dres.api.rest.handler.evaluation.client

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import dev.dres.api.rest.types.evaluation.ApiEvaluationInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.run.DbEvaluation
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] used to list all ongoing [DbEvaluation]s available to the current user.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ClientListEvaluationsHandler(store: TransientEntityStore): AbstractEvaluationClientHandler(store), GetRestHandler<List<ApiEvaluationInfo>> {

    override val route = "client/evaluation/list"

    @OpenApi(
        summary = "Lists an overview of all evaluation runs visible to the current client.",
        path = "/api/v2/client/evaluation/list",
        tags = ["Evaluation Client"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiEvaluationInfo>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiEvaluationInfo> = this.store.transactional(true) { tx ->
        getRelevantManagers(ctx).map { ApiEvaluationInfo(
            id = it.id,
            name = it.name,
            templateId = it.template.id,
            templateDescription = it.template.description,
            when (it) {
                is InteractiveAsynchronousRunManager -> ApiEvaluationType.ASYNCHRONOUS
                is InteractiveSynchronousRunManager -> ApiEvaluationType.SYNCHRONOUS
                else -> TODO()
            },
            properties = it.runProperties,
            teams = emptyList(),
            tasks = emptyList()
        ) }
    }
}
