package dev.dres.run.eventstream

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.log.QueryEventLog
import dev.dres.data.model.log.QueryResultLog
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.submissions.Submission

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
sealed class StreamEvent(var timeStamp : Long = System.currentTimeMillis(), var session: String? = null)
class TaskStartEvent(val runId: EvaluationId, val taskId: EvaluationId, val taskTemplate: DbTaskTemplate) : StreamEvent()
class TaskEndEvent(val runId: EvaluationId, val taskId: EvaluationId) : StreamEvent()
class RunStartEvent(val runId: EvaluationId, val description: DbEvaluationTemplate) : StreamEvent()
class RunEndEvent(val runId: EvaluationId) : StreamEvent()
class SubmissionEvent(session: String, val runId: EvaluationId, val taskId: EvaluationId?, val submission : Submission) : StreamEvent(session = session)
class QueryEventLogEvent(session: String?, val runId: EvaluationId, val queryEventLog: QueryEventLog) : StreamEvent(session = session)
class QueryResultLogEvent(session: String?, val runId: EvaluationId, val queryResultLog: QueryResultLog) : StreamEvent(session = session)
class InvalidRequestEvent(session: String?, val runId: EvaluationId, val requestData: String) : StreamEvent(session = session)