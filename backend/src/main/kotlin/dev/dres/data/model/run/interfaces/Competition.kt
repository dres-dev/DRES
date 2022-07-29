package dev.dres.data.model.run.interfaces

import dev.dres.data.model.Entity
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.RunProperties

/**
 * Represents a [Competition] that a DRES user or client takes place in and that groups several [Task]s
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Competition: Run, Entity {
    /** The unique [CompetitionId] that identifies this [Competition]. Used by the persistence layer. */
    override var id: CompetitionId

    /** The name human readable of this [Competition]. */
    val name: String

    /** Reference to the [CompetitionDescription] that describes the content of this [Competition]. */
    val description: CompetitionDescription

    /** Collection of [Task]s that make up this [Competition]. */
    val tasks: List<Task>

    /** Various run-specific settings */
    var properties: RunProperties
}