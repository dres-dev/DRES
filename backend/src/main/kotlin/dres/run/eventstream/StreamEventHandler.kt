package dres.run.eventstream

interface StreamEventHandler {

    fun handle(event: StreamEvent)

}