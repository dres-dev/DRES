package dev.dres.api.rest.types.system

/**
 * The current timestamp.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class CurrentTime(val timeStamp: Long = System.currentTimeMillis())