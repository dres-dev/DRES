package dev.dres.data.model.template.task.options

object Defaults {
    /** Default value for [Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM]. Defaults to the rule being applied during the final 5 seconds. */
    const val PROLONG_ON_SUBMISSION_LIMIT_DEFAULT = 5

    /** Default value for [Parameters.PROLONG_ON_SUBMISSION_BY_PARAM]. Defaults to a prolongation of 5 seconds. */
    const val PROLONG_ON_SUBMISSION_BY_DEFAULT = 5

    const val PROLONG_ON_SUBMISSION_CORRECT_DEFAULT = false
}