package dev.dres.run.eventstream

import dev.dres.data.model.UID
import dev.dres.data.model.log.QueryEventLog
import dev.dres.data.model.log.QueryResultLog
import dev.dres.data.model.run.Submission

sealed class StreamEvent(var timeStamp : Long = System.currentTimeMillis(), var session: String)

class SubmissionEvent(session: String, val submission : Submission) : StreamEvent(session = session)

class QueryEventLogEvent(session: String, val runId: UID, val queryEventLog: QueryEventLog) : StreamEvent(session = session)
class QueryResultLogEvent(session: String, val runId: UID, val queryResultLog: QueryResultLog) : StreamEvent(session = session)
class InvalidRequestEvent(session: String, val runId: UID, val requestData: String) : StreamEvent(session = session)
