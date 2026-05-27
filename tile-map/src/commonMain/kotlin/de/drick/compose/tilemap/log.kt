package de.drick.compose.tilemap

object LogSettings {
    var logPlatform: ((msg: () -> String, error: Throwable?) -> Unit)? = null
}

internal fun log(error: Throwable? = null, msg: () -> String) {
    LogSettings.logPlatform?.invoke(msg, error)
}

internal fun log(error: Throwable) {
    log(error = error, msg = { error.message ?: "Unknown error" })
}

internal fun log(msg: String?, error: Throwable? = null) = logPlatform(error, msg = { msg ?: "Unknown error" })

internal fun logPlatform(error: Throwable? = null, msg: () -> String) {
    LogSettings.logPlatform?.invoke(msg, error)
}
