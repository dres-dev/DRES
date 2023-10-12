package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.task.options.DbSubmissionOption
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.singleOrNull
import kotlinx.dnq.query.size

/**
 * A [SubmissionFilter] that checks that the maximum number of wrong submissions has not been exceeded for the submitting team and task.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class MaximumWrongPerTeamFilter(private val limit: Int = PARAMETER_KEY_LIMIT_DEFAULT) : AbstractSubmissionFilter("Maximum number of incorrect submissions ($limit) exceeded for the team.") {

    init {
        require(limit > 0) { "Value of limit must be greater than zero for MaximumWrongPerTeamFilter." }
    }

    companion object {
        /** The name for the limit parameter. */
        val PARAMETER_KEY_LIMIT = "${DbSubmissionOption.LIMIT_WRONG_PER_TEAM.description}.limit"

        /** The default value for the limit parameter. */
        const val PARAMETER_KEY_LIMIT_DEFAULT = 3
    }

    /**
     * Constructor using the parameters map.
     *
     * @param parameters The parameters map.
     */
    constructor(parameters: Map<String, String>) : this(parameters[PARAMETER_KEY_LIMIT]?.toIntOrNull() ?: PARAMETER_KEY_LIMIT_DEFAULT)

    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeds.
     *
     * Requires an ongoing transaction!
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean {
        require(t.teamId != null) { "Submission ${t.submissionId} is not associated with a team." }
        for (answerSet in t.answerSets) {
            require(answerSet.taskId != null) { "Answer for submission ${t.submissionId} is not associated with a task." }
            val task = DbTask.filter { it.id eq answerSet.taskId }.singleOrNull() ?: throw IllegalStateException("The specified task ${answerSet.taskId} does not exist in the database.")
            if (task.answerSets.filter { (it.status eq DbVerdictStatus.WRONG) and (it.submission.team.id eq t.teamId) }.size() >= this.limit) {
                return false
            }
        }
        return true
    }
}
