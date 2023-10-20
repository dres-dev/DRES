package dev.dres.run.eventstream

interface StreamEventHandler {

    fun handleStreamEvent(event: StreamEvent)

}