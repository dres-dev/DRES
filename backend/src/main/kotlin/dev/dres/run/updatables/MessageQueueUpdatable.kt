package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * An internal queue of [ServerMessage]s that are due for sending by the [RunManager].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class MessageQueueUpdatable(private val executor: RunExecutor) : Updatable {

    /** The [MessageQueueUpdatable] always belongs to the [Phase.FINALIZE]. */
    override val phase: Phase = Phase.FINALIZE

    /** Internal queue of all [ServerMessage] that are due for sending. */
    private val messageQueue = ConcurrentLinkedQueue<Pair<TeamId?, ServerMessage>>()

    /** Sends all [ServerMessage]s that are due for sending. */
    override fun update(status: RunManagerStatus) {
        var message: Pair<TeamId?, ServerMessage>? = this.messageQueue.poll()
        while (message != null) {
            if (message.first == null) {
                this.executor.broadcastWsMessage(message.second.evaluationId, message.second)
            } else {
                this.executor.broadcastWsMessage(message.second.evaluationId, message.first!!, message.second)
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