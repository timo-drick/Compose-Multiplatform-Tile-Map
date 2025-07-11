package de.drick.compose.tilemap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform