package de.drick.compose.tilemap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import io.ktor.client.HttpClient
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.collections.set

const val DEBUG_MODE_ENABLED = false

val tileProviderOsm = TileProvider(
    name = "Open Street Map",
    tileLoaderUrl = { pos ->
        URLBuilder("https://tile.openstreetmap.org").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), "${pos.tileY}.png")
        }.build()
    }
)

fun tileProviderMapBoxSat(mapBoxToken: String) = TileProvider(
    name = "MapBox",
    tileLoaderUrl = { pos ->
        URLBuilder("https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/512").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), pos.tileY.toString())
            parameters.append("access_token", mapBoxToken)
        }.build()
    }
)

fun tileProviderMapBoxLight(mapBoxToken: String) = TileProvider(
    name = "MapBox Light",
    tileLoaderUrl = { pos ->
        URLBuilder("https://api.mapbox.com/styles/v1/mapbox/light-v11/tiles/512").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), pos.tileY.toString())
            parameters.append("access_token", mapBoxToken)
        }.build()
    }
)
fun tileProviderMapBoxDark(mapBoxToken: String) = TileProvider(
    name = "MapBox Dark",
    tileLoaderUrl = { pos ->
        URLBuilder("https://api.mapbox.com/styles/v1/mapbox/dark-v11/tiles/512").apply {
            appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), pos.tileY.toString())
            parameters.append("access_token", mapBoxToken)
        }.build()
    }
)

private val client by lazy {
    HttpClient {
        if (DEBUG_MODE_ENABLED) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client $message")
                    }
                }
                level = LogLevel.HEADERS
            }
        }
    }
}

@Serializable
data class ElevationResult(
    val elevation: List<Double>
)

/**
 * https://open-meteo.com/en/docs/elevation-api
 */
suspend fun getElevation(point: GeoPoint): Double? {
    val url = URLBuilder("https://api.open-meteo.com/v1/elevation").apply {
        parameters.append("latitude", point.latitude.toString())
        parameters.append("longitude", point.longitude.toString())
    }.build()
    val response = client.request(url)
    if (response.status.isSuccess()) {
        val resultString = response.bodyAsText()
        val result = Json.decodeFromString<ElevationResult>(resultString)
        return result.elevation.firstOrNull()
    } else {
        return null
    }
}

class TileProvider(
    val name: String,
    private val tileLoaderUrl: (TilePos) -> Url,
) {
    override fun toString() = name
    val inMemoryCache = mutableMapOf<TilePos, ByteArray>()
    suspend fun loadTile(pos: TilePos) = withContext(Dispatchers.Default) {
        val url = tileLoaderUrl(pos.wrap())
        val response = client.request(url)
        if (response.status.isSuccess()) {
            val bodyBytes = response.bodyAsBytes()
            try {
                val image = bodyBytes.decodeToImageBitmap()
                if (image.height > 0 && image.width > 0) {
                    inMemoryCache[pos] = bodyBytes
                    image
                } else {
                    null
                }
            } catch (int: CancellationException) {
                throw int
            } catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
                null
            }
        } else {
            null
        }
    }
    fun cachedTile(pos: TilePos): ImageBitmap? =
        inMemoryCache[pos]?.decodeToImageBitmap()

    /**
     * Search for a cached tile at a lower zoom level that contains the given tile position.
     * Returns the parent tile image and the zoom level it was found at, or null if none found.
     */
    fun cachedParentTile(pos: TilePos, minZoom: Int = 1): Pair<ImageBitmap, Int>? {
        for (z in (pos.zoom - 1) downTo minZoom) {
            val scale = 1 shl (pos.zoom - z)
            val parentX = pos.tileX / scale
            val parentY = pos.tileY / scale
            val parentPos = TilePos(z, parentX.toDouble(), parentY.toDouble())
            val image = cachedTile(parentPos)
            if (image != null) {
                return Pair(image, z)
            }
        }
        return null
    }
}
