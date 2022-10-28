package dev.dres.utilities.extensions

import dev.dres.api.rest.handler.SessionId
import dev.dres.data.model.UID
import java.util.*
import java.util.regex.Matcher

/**
 * Converts a [String] to a [UID].
 *
 * @return [UID]
 */
fun String.UID(): UID = UID(UUID.fromString(this))

/**
 * Converts a [String] to a [UID].
 *
 * @return [UID]
 */
fun String.cleanPathString() = this.trim().replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")))

/**
 * Converts a [String] to a [UID].
 *
 * @return [UID]
 */
fun String.toPathParamKey() = "{$this}"