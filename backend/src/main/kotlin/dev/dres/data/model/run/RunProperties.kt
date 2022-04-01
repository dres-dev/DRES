package dev.dres.data.model.run

data class RunProperties(
    val participantCanView: Boolean = true,
    val shuffleTasks: Boolean = false //is only used for asynchronous runs
)
