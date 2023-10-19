package dev.dres.utilities.extensions

import java.util.regex.Matcher

/**
 * Converts a [String] to a [EvaluationId].
 *
 * @return [EvaluationId]
 */
fun String.cleanPathString() = this.trim().replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")))

/**
 * Converts a [String] to a [EvaluationId].
 *
 * @return [EvaluationId]
 */
fun String.toPathParamKey() = "{$this}"