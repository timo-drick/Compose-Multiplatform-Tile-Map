package de.drick.compose.tilemap.sample

import android.util.Log

actual fun log(msg: String) {
    Log.d("MultiplatformTileMap", msg)
}