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
import kotlin.math.max
import kotlin.math.min

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
    name = "MapBox Dark",
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

val tileProviderDipul = TileProvider(
    name = "Dipul",
    tileLoaderUrl = { pos ->
        URLBuilder("https://sgx.geodatenzentrum.de/wms_topplus_open").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("TRANSPARENT", "TRUE")
                append("LAYERS", "web_scale_grau")
                append("TILED", "true")
                append("WIDTH", "256")
                append("HEIGHT", "256")
                append("CRS", "EPSG:3857")
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

val layer = listOf(
    "flugplaetze", "flughaefen", "kontrollzonen", "flugbeschraenkungsgebiete", "bundesautobahnen", "bundesstrassen",
    "bahnanlagen", "binnenwasserstrassen", "seewasserstrassen", "schifffahrtsanlagen", "wohngrundstuecke", "freibaeder",
    "industrieanlagen", "kraftwerke", "umspannwerke", "stromleitungen", "windkraftanlagen", "justizvollzugsanstalten",
    "militaerische_anlagen", "labore", "behoerden", "diplomatische_vertretungen", "internationale_organisationen",
    "polizei", "sicherheitsbehoerden", "krankenhaeuser", "nationalparks", "naturschutzgebiete", "vogelschutzgebiete",
    "ffh-gebiete", "temporaere_betriebseinschraenkungen", "modellflugplaetze", "haengegleiter"
).map { "dipul:$it" }


val tileProviderDipulZones = TileProvider(
    name = "Dipul Zones",
    tileLoaderUrl = { pos ->
        URLBuilder("https://uas-betrieb.de/geoservices/dipul/wms").apply {
            parameters.apply {
                append("REQUEST", "GetMap")
                append("SERVICE", "WMS")
                append("STYLES", "")
                append("VERSION", "1.3.0")
                append("FORMAT", "image/png")
                append("FORMAT_OPTIONS", "dpi:180")
                append("TRANSPARENT", "TRUE")
                append("TILED", "true")
                append("WIDTH", "512")
                append("HEIGHT", "512")
                append("CRS", "EPSG:3857")

                append("LAYERS", layer.joinToString(","))
                with(calculateBoundingBox(pos)) {
                    append("BBOX", "$startX,$startY,$endX,$endY")
                }
            }
        }.build()
    }
)

data class BBox(val startX: Int, val startY: Int, val endX: Int, val endY: Int)

fun calculateBoundingBox(pos: TilePos): BBox {
    val start = pos.toGeoPoint() // Left top corner of the tile
    val end = pos.copy(x = pos.x + 1, y = pos.y + 1).toGeoPoint() // Right bottom corner of the tile
    val meterStart = start.toMeter()
    val meterEnd = end.toMeter()
    val startx = min(meterEnd.x, meterStart.x)
    val starty = min(meterEnd.y, meterStart.y)
    val endx = max(meterStart.x, meterEnd.x)
    val endy = max(meterStart.y, meterEnd.y)
    return BBox(
        startX = startx.toInt(),
        startY = starty.toInt(),
        endX = endx.toInt(),
        endY = endy.toInt()
    )
}
