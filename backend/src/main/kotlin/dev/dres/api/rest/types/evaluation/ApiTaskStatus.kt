package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.DbTaskStatus


/** The collection auf statuses an [ApiTask] can be in.
*
* API version of [DbTaskStatus]
*
* @author Ralph Gasser
* @version 1.0.0
*/
enum class ApiTaskStatus {
    NO_TASK, CREATED, PREPARING, RUNNING, ENDED, IGNORED;

    /**
     * Converts this [ApiTaskStatus] to a [DbTaskStatus] representation. Requires an ongoing transaction.
     *
     * @return [Db]
     */
    fun toDb(): DbTaskStatus = when(this) {
        CREATED -> DbTaskStatus.CREATED
        PREPARING -> DbTaskStatus.PREPARING
        RUNNING -> DbTaskStatus.RUNNING
        ENDED -> DbTaskStatus.ENDED
        IGNORED -> DbTaskStatus.IGNORED
        else -> throw IllegalArgumentException("The API task status $this cannot be converted to a persistent representation.")
    }
}