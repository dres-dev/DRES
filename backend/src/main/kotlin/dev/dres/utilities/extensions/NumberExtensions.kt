package dev.dres.utilities.extensions

import java.text.SimpleDateFormat
import java.util.Date

fun Long.toDateString() : String = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss").format(Date(this))