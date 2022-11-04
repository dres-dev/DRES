package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.RunType

/**
 *
 */
enum class ApiRunType(val type: RunType) {
    SYNCHRONOUS(RunType.INTERACTIVE_SYNCHRONOUS),
    ASYNCHRONOUS(RunType.INTERACTIVE_ASYNCHRONOUS)
}