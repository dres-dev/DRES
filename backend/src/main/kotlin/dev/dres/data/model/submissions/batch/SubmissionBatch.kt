package dev.dres.data.model.submissions.batch

import dev.dres.data.model.submissions.aspects.OriginAspect

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface SubmissionBatch<R: ResultBatch<*>> : OriginAspect {
    val results : Collection<R>
}
