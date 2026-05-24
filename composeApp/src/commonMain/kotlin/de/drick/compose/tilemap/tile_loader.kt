package de.drick.compose.tilemap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import de.drick.tools.debugModeEnabled
import de.drick.tools.log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min


val tileProviderOsm = TileProvider(
    name = "Open Street Map",
    tileLoaderUrl = { pos ->
        URLBuilder("https://tile.openstreetmap.org").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), "${pos.tileY}.png")
        }.build()
    }
)

val tileProviderMapBox = TileProvider(
    name = "MapBox",
    tileLoaderUrl = { pos ->
        URLBuilder("https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/512").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), pos.tileY.toString())
            parameters.append("access_token", mapboxToken)
        }.build()
    }
)



private val client by lazy {
    HttpClient {
        if (debugModeEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        de.drick.tools.log("HTTP Client $message")
                    }
                }
                level = LogLevel.HEADERS
            }
        }
    }
}

class TileProvider(
    val name: String,
    private val tileLoaderUrl: (TilePos) -> Url,
) {
    override fun toString() = name
    val inMemoryCache = mutableMapOf<TilePos, ByteArray>()
    suspend fun loadTile(pos: TilePos) = withContext(Dispatchers.Default) {
        val url = tileLoaderUrl(pos)
        val response = client.request(url)
        if (response.status.isSuccess()) {
            val bodyBytes = response.bodyAsBytes()
            try {
                val image = bodyBytes.decodeToImageBitmap()
                if (image.height > 0 && image.width > 0) {
                    inMemoryCache[pos] = bodyBytes
                    image
                } else {
                    log("$name invalid data: ${response.bodyAsText()}")
                    null
                }
            } catch (int: CancellationException) {
                throw int
            } catch (err: Throwable) {
                log("No valid image data:\n${response.bodyAsText()}", err)
                null
            }
        } else {
            log("$name: Failed to load tile from $url, status: ${response.status}")
            null
        }
    }
    fun cachedTile(pos: TilePos): ImageBitmap? =
        inMemoryCache[pos]?.decodeToImageBitmap()
}
