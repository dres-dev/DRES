package dres.run.updatables

import dres.api.rest.types.run.websocket.ServerMessage
import dres.run.RunExecutor
import dres.run.RunManagerStatus
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A internal queue of [ServerMessage]s that are due for sending by the [RunManager].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class MessageQueueUpdatable(val executor: RunExecutor) : Updatable {

    /** The [Phase] this [MessageQueueUpdatable] belongs to. */
    override val phase: Phase = Phase.FINALIZE

    /** Internal queue of all [ServerMessage] that are due for sending. */
    private val messageQueue = ConcurrentLinkedQueue<ServerMessage>()

    /** Sends all [ServerMessage]s that are due for sending. */
    override fun update(status: RunManagerStatus) {
        var message: ServerMessage? = this.messageQueue.poll()
        while (message != null) {
            this.executor.broadcastWsMessage(message.runId, message)
            message = this.messageQueue.poll()
        }
    }

    /**
     * Enqueues a [ServerMessage] for later sending.
     *
     * @param message The [ServerMessage] to enqueue.
     */
    fun enqueue(message: ServerMessage) = this.messageQueue.offer(message)

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = true
}