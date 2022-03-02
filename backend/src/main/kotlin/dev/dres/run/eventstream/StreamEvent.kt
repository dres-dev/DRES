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

/**
 * Event indicating that the task was started.
 */
class TaskStartEvent(val runId: UID, val taskId: UID, val taskDescription: TaskDescription) : StreamEvent()

/**
 * Event indicating that the task was finished.
 */
class TaskEndEvent(val runId: UID, val taskId: UID) : StreamEvent()

/**
 * Event indicating that a run was started. The competition this run is an instance of is provided.
 */
class RunStartEvent(val runId: UID, val description: CompetitionDescription) : StreamEvent()

/**
 * Event indicating that a run was finished.
 */
class RunEndEvent(val runId: UID) : StreamEvent()

/**
 * Event indicating that a submission was received.
 */
class SubmissionEvent(session: String, val runId: UID, val taskId: UID?, val submission : Submission) : StreamEvent(session = session)

/**
 * Event indicating that a query event was logged (aka interaction log)
 */
class QueryEventLogEvent(session: String, val runId: UID, val queryEventLog: QueryEventLog) : StreamEvent(session = session)

/**
 * Event indicating that a query result was logged (aka result log)
 */
class QueryResultLogEvent(session: String, val runId: UID, val queryResultLog: QueryResultLog) : StreamEvent(session = session)

/**
 * Event indicating that a non-usable log was submitted (and thus was not properly stored).
 * One reason for this event could be incomplete log data (e.g. JSON not properly closed)
 */
class InvalidRequestEvent(session: String, val runId: UID, val requestData: String) : StreamEvent(session = session)
