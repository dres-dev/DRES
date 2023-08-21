package dev.dres.data.model.run

/**
 *
 */
data class RunProperties ( // TODO shoudln't we move this to db and api ?
    val participantCanView: Boolean = true,
    val shuffleTasks: Boolean = false, //is only used for asynchronous runs
    val allowRepeatedTasks: Boolean = false,
    val limitSubmissionPreviews: Int = -1
)
