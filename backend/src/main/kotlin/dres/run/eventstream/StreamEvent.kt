package dres.run.eventstream

import dres.data.model.log.QueryEventLog
import dres.data.model.log.QueryResultLog
import dres.data.model.run.Submission

sealed class StreamEvent(var timeStamp : Long = System.currentTimeMillis(), var session: String)

class SubmissionEvent(session: String, val submission : Submission) : StreamEvent(session = session)

class QueryEventLogEvent(session: String, val runId: String, val queryEventLog: QueryEventLog) : StreamEvent(session = session)
class QueryResultLogEvent(session: String, val runId: String, val queryResultLog: QueryResultLog) : StreamEvent(session = session)
class InvalidRequestEvent(session: String, val runId: String, val requestData: String) : StreamEvent(session = session)
