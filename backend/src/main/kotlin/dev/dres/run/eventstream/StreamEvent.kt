package dev.dres.run.eventstream

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.log.QueryEventLog
import dev.dres.data.model.log.QueryResultLog
import dev.dres.data.model.run.interfaces.EvaluationId


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
sealed class StreamEvent(var timeStamp : Long = System.currentTimeMillis(), var session: String? = null)
class TaskStartEvent(val runId: EvaluationId, val taskId: EvaluationId, val taskTemplate: ApiTaskTemplate) : StreamEvent()
class TaskEndEvent(val runId: EvaluationId, val taskId: EvaluationId) : StreamEvent()
class RunStartEvent(val runId: EvaluationId, val description: ApiEvaluationTemplate) : StreamEvent()
class RunEndEvent(val runId: EvaluationId) : StreamEvent()
class SubmissionEvent(session: String, val runId: EvaluationId, val submission : ApiClientSubmission) : StreamEvent(session = session)
class QueryEventLogEvent(session: String?, val runId: EvaluationId, val queryEventLog: QueryEventLog) : StreamEvent(session = session)
class QueryResultLogEvent(session: String?, val runId: EvaluationId, val queryResultLog: QueryResultLog) : StreamEvent(session = session)
class InvalidRequestEvent(session: String?, val runId: EvaluationId, val requestData: String) : StreamEvent(session = session)