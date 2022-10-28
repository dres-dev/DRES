package dev.dres.data.model.competition.interfaces

import dev.dres.run.filter.SubmissionFilter

/**
 * A factory for [SubmissionFilter]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface SubmissionFilterFactory {
    /**
     * Generates and returns a [SubmissionFilter]. Depending on the implementation, the returned instance
     * is a new instance or can re-used.
     *
     * @return [SubmissionFilter]
     */
    fun newFilter(): SubmissionFilter
}