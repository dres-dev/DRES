package dev.dres.run.updatables

import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.data.model.competition.TeamId
import dev.dres.run.RunExecutor
import dev.dres.run.RunManagerStatus
import dev.dres.utilities.extensions.UID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A internal queue of [ServerMessage]s that are due for sending by the [RunManager].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class MessageQueueUpdatable(private val executor: RunExecutor) : Updatable {

    /** The [Phase] this [MessageQueueUpdatable] belongs to. */
    override val phase: Phase = Phase.FINALIZE

    /** Internal queue of all [ServerMessage] that are due for sending. */
    private val messageQueue = ConcurrentLinkedQueue<Pair<TeamId?, ServerMessage>>()

    /** Sends all [ServerMessage]s that are due for sending. */
    override fun update(status: RunManagerStatus) {
        var message: Pair<TeamId?, ServerMessage>? = this.messageQueue.poll()
        while (message != null) {
            if (message.first == null) {
                this.executor.broadcastWsMessage(message.second.runId.UID(), message.second)
            } else {
                this.executor.broadcastWsMessage(message.second.runId.UID(), message.first!!, message.second)
            }
            message = this.messageQueue.poll()
        }
    }

    /**
     * Enqueues a [ServerMessage] for later sending.
     *
     * @param message The [ServerMessage] to enqueue.
     */
    fun enqueue(message: ServerMessage) = this.messageQueue.offer(null to message)

    fun enqueue(message: ServerMessage, recipient: TeamId) = this.messageQueue.offer(recipient to message)

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = true
}