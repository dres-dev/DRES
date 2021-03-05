package dev.dres.data.model.submissions.aspects

import dev.dres.data.model.run.AbstractInteractiveTask

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface BaseSubmissionAspect : StatusAspect, ItemAspect, OriginAspect {
    var task: AbstractInteractiveTask?
    val timestamp: Long
}