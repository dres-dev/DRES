package dev.dres.data.model.template.task.options

object Parameters {
    /**
     * Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Int] value expected.
     *
     * The parameter determines how many milliseconds before the end of a task the option should be applied.
     * If a task has a total duration of 500s, then a value of 5s means that submissions arriving between 495s and 500s
     * may trigger prolongation of the task.
     */
    const val PROLONG_ON_SUBMISSION_LIMIT_PARAM = "PROLONG_ON_SUBMISSION_LIMIT"

    /** Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Int] value expected. Determines by how many seconds a task should be prolonged. */
    const val PROLONG_ON_SUBMISSION_BY_PARAM = "PROLONG_ON_SUBMISSION_BY"

    /** Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Boolean] value expected. If true, task will only be prolonged for correct submissions. */
    const val PROLONG_ON_SUBMISSION_CORRECT_PARAM = "PROLONG_ON_SUBMISSION_CORRECT"
}