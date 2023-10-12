package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.task.options.DbSubmissionOption
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * A [SubmissionFilter] that checks that the specified submission rate is not exceeded.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class SubmissionRateFilter(private val minDelayMs: Int = PARAMETER_KEY_DELAY_DEFAULT) : AbstractSubmissionFilter("Not enough time has passed since last submission. Delay needs to be at least $minDelayMs ms.") {

    companion object {
        /** The name for the delay parameter. */
        val PARAMETER_KEY_DELAY = "${DbSubmissionOption.MINIMUM_TIME_GAP.description}.delay"

        /** The default value for the limit parameter. */
        const val PARAMETER_KEY_DELAY_DEFAULT = 500
    }

    /**
     * Constructor using the parameters map.
     *
     * @param parameters The parameters map.
     */
    constructor(parameters: Map<String, String>) : this(parameters[PARAMETER_KEY_DELAY]?.toIntOrNull() ?: PARAMETER_KEY_DELAY_DEFAULT)

    /** Internal map of [UserId] to submission timestamp. */
    private val submissions = ConcurrentHashMap<UserId,Long>()

    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeds.
     *
     * Requires an ongoing transaction!
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean {
        require(t.userId != null) { "Submission ${t.submissionId} is not associated with a user." }
        val lastSubmission = this.submissions[t.userId]
        val currentSubmission = System.currentTimeMillis()
        this.submissions[t.userId!!] = currentSubmission
        return lastSubmission == null || (currentSubmission - lastSubmission <= minDelayMs)
    }
}
