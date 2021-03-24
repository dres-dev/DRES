package dev.dres.data.model.competition.options

/**
 * Named parameters and defaults for [SimpleOption]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object SimpleOptionParameters {
    /**
     * Key for [SimpleOption.PROLONG_ON_SUBMISSION]; [Int] value expected. The parameter determines how many milliseconds
     * before the end of a task a [SimpleOption.PROLONG_ON_SUBMISSION] [Option] should be applied. If a task has a total duration
     * of 500s, then a value of 5s means that submissions arriving between 495s and 500s may trigger prolongation of the task.
     */
    const val PROLONG_ON_SUBMISSION_LIMIT_PARAM = "limit"

    /** Default value for [PROLONG_ON_SUBMISSION_LIMIT_PARAM]. Defaults to the rule being applied during the final 5 seconds. */
    const val PROLONG_ON_SUBMISSION_LIMIT_DEFAULT = 5

    /** Key for [SimpleOption.PROLONG_ON_SUBMISSION]; [Int] value expected. Determines by how many seconds a task should be prolonged. */
    const val PROLONG_ON_SUBMISSION_BY_PARAM = "prolong_by"

    /** Default value for [PROLONG_ON_SUBMISSION_BY_PARAM]. Defaults to a prolongation of 5 seconds. */
    const val PROLONG_ON_SUBMISSION_BY_DEFAULT = 5

    /** Key for [SimpleOption.PROLONG_ON_SUBMISSION]; [Int] value expected. Determines by how many seconds a task should be prolonged. */
    const val PROLONG_ON_SUBMISSION_CORRECT_PARAM = "correct_only"

    /** Default value for [PROLONG_ON_SUBMISSION_BY_PARAM]. Defaults to a prolongation of 5 seconds. */
    const val PROLONG_ON_SUBMISSION_CORRECT_DEFAULT = false
}