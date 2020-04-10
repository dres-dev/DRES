package dres.data.model.competition.interfaces

import dres.data.model.basics.MediaItem

/**
 * A [TaskDescription] looking for a specific [MediaItem].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface MediaItemTaskDescription : TaskDescription {
    val item: MediaItem
}