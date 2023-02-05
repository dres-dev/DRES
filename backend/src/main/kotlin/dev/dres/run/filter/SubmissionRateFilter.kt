package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission

class SubmissionRateFilter(private val minDelayMS: Int = 500) : SubmissionFilter {

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("delay", "500").toIntOrNull() ?: 500)

    override val reason = "Not enough time has passed since last submission, gap needs to be at least $minDelayMS ms"

    override fun test(t: Submission): Boolean {
        TODO("Not sure about the semantic here. Do we need a data model extension for this?")
        /*val mostRecentSubmissionTime = t.task!!.submissions.maxByOrNull { it.timestamp }?.timestamp ?: 0
        return (t.timestamp - mostRecentSubmissionTime) >= minDelayMS*/
    }
}