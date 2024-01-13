package dev.dres.data.model.run

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
data class ApiRunProperties (
    val participantCanView: Boolean = true,
    val shuffleTasks: Boolean = false, //is only used for asynchronous runs
    val allowRepeatedTasks: Boolean = false,
    val limitSubmissionPreviews: Int = -1
)
