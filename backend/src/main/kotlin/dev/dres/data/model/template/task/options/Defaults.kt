package dev.dres.data.model.template.task.options

object Defaults {
    /** Default value for [Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM]. Defaults to the rule being applied during the final 5 seconds. */
    const val PROLONG_ON_SUBMISSION_LIMIT_DEFAULT = 5

    /** Default value for [Parameters.PROLONG_ON_SUBMISSION_BY_PARAM]. Defaults to a prolongation of 5 seconds. */
    const val PROLONG_ON_SUBMISSION_BY_DEFAULT = 5

    const val PROLONG_ON_SUBMISSION_CORRECT_DEFAULT = false

    /** Default interval for scoreboard updates in ms. TODO: Make configurable. */
    const val SCOREBOARD_UPDATE_INTERVAL_DEFAULT = 5000L

    /**
     * Default timeout for viewers in ms, measured from the moment a task enters PREPARING.
     *
     * Deliberately a multiple of the viewer's own re-request cadence. The browser viewer re-requests the
     * current task every 30 s, so a 30 s timeout expired at almost exactly the moment the viewer would
     * have retried, and a slow page load silently defeated the "wait for every viewer" contract. Three
     * cadence cycles leave room for a reload without making the wait unbounded; a real timeout is logged
     * with the viewers that never reported ready.
     */
    const val VIEWER_TIMEOUT_DEFAULT = 90_000L
}