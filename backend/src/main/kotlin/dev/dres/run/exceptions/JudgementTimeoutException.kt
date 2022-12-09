package dev.dres.run.exceptions

/**
 * A [IllegalArgumentException] to throw whenever a [JudgementValidator] cannot judge due to a timeout.
 * In particular, this exception should be thrown to indicate the judgement
 * verdict received will not be applied, since it took too long.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
class JudgementTimeoutException(msg: String) : IllegalArgumentException(msg)
