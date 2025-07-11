package de.drick.tools

var debugModeEnabled = true

fun log(error: Throwable? = null, msg: () -> String) {
    if (debugModeEnabled) logPlatform(error, msg)
}

fun log(error: Throwable) {
    log(error = error, msg = { error.message ?: "Unknown error" })
}

fun log(msg: String?, error: Throwable? = null) = log(error, { msg ?: "Unknown error" })

expect fun logPlatform(error: Throwable? = null, msg: () -> String)
