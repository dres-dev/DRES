package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.task.options.DbSubmissionOption

class SubmissionRateFilter(private val minDelayMS: Int = 500) : SubmissionFilter {

    companion object {
        val PARAMETER_KEY_DELAY = "${DbSubmissionOption.MINIMUM_TIME_GAP.description}.delay"
    }

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault(PARAMETER_KEY_DELAY, "500").toIntOrNull() ?: 500)

    override val reason = "Not enough time has passed since last submission, gap needs to be at least $minDelayMS ms"

    override fun test(t: ApiSubmission): Boolean {
        // FIXME
        TODO("Not sure about the semantic here. Do we need a data model extension for this?")
        /*val mostRecentSubmissionTime = t.task!!.submissions.maxByOrNull { it.timestamp }?.timestamp ?: 0
        return (t.timestamp - mostRecentSubmissionTime) >= minDelayMS*/
    }
}
