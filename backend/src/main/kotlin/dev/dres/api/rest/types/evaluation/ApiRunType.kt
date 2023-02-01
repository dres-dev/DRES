package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.EvaluationType

/**
 *
 */
enum class ApiRunType(val type: EvaluationType) {
    SYNCHRONOUS(EvaluationType.INTERACTIVE_SYNCHRONOUS),
    ASYNCHRONOUS(EvaluationType.INTERACTIVE_ASYNCHRONOUS),
    NON_INTERACTIVE(EvaluationType.NON_INTERACTIVE)
}