package dev.dres.utilities.extensions

import java.io.File

fun File.isEmpty(): Boolean {
    return !this.exists() || this.length() == 0L
}