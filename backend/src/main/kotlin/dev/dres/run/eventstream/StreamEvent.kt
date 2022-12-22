package dev.dres.run.eventstream

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.log.QueryEventLog
import dev.dres.data.model.log.QueryResultLog
import dev.dres.data.model.submissions.Submission

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
sealed class StreamEvent(var timeStamp : Long = System.currentTimeMillis(), var session: String? = null)


class TaskStartEvent(val runId: UID, val taskId: UID, val taskDescription: TaskDescription) : StreamEvent()
class TaskEndEvent(val runId: UID, val taskId: UID) : StreamEvent()
class RunStartEvent(val runId: UID, val description: CompetitionDescription) : StreamEvent()
class RunEndEvent(val runId: UID) : StreamEvent()


class SubmissionEvent(session: String, val runId: UID, val taskId: UID?, val submission : Submission) : StreamEvent(session = session)
class QueryEventLogEvent(session: String?, val runId: UID, val queryEventLog: QueryEventLog) : StreamEvent(session = session)
class QueryResultLogEvent(session: String?, val runId: UID, val queryResultLog: QueryResultLog) : StreamEvent(session = session)
class InvalidRequestEvent(session: String?, val runId: UID, val requestData: String) : StreamEvent(session = session)