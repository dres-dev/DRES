package dev.dres.utilities.extensions

import dev.dres.data.model.UID
import java.util.regex.Matcher

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