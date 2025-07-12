package de.drick.compose.tilemap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import de.drick.tools.log
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

fun osmFree(pos: TilePos) = URLBuilder("https://tile.openstreetmap.org").apply {
    appendPathSegments(pos.zoom.toString(), pos.tileX.toString(), "${pos.tileY}.png")
}.build()

val layer = listOf(
    "flugplaetze", "flughaefen", "kontrollzonen", "flugbeschraenkungsgebiete", "bundesautobahnen", "bundesstrassen",
    "bahnanlagen", "binnenwasserstrassen", "seewasserstrassen", "schifffahrtsanlagen", "wohngrundstuecke", "freibaeder",
    "industrieanlagen", "kraftwerke", "umspannwerke", "stromleitungen", "windkraftanlagen", "justizvollzugsanstalten",
    "militaerische_anlagen", "labore", "behoerden", "diplomatische_vertretungen", "internationale_organisationen",
    "polizei", "sicherheitsbehoerden", "krankenhaeuser", "nationalparks", "naturschutzgebiete", "vogelschutzgebiete",
    "ffh-gebiete", "temporaere_betriebseinschraenkungen", "modellflugplaetze", "haengegleiter"
).map { "dipul:$it" }

val dipulBaseUrl = "https://sgx.geodatenzentrum.de/wms_topplus_open?REQUEST=GetMap&SERVICE=WMS&VERSION=1.3.0&FORMAT=image/png&STYLES=&TRANSPARENT=TRUE&LAYERS=web_scale_grau&TILED=true&WIDTH=256&HEIGHT=256&CRS=EPSG:3857"
val dipulLayerBaseUrl = "    https://uas-betrieb.de/geoservices/dipul/wms?REQUEST=GetMap&SERVICE=WMS&VERSION=1.3.0&FORMAT=image%2Fpng&STYLES=&TRANSPARENT=TRUE&TILED=true&FORMAT_OPTIONS=dpi%3A180&WIDTH=512&HEIGHT=512&CRS=EPSG%3A3857"
fun dipul(pos: TilePos) = URLBuilder(dipulBaseUrl).apply {
    //parameters.append("LAYERS", layer.joinToString(","))
    val start = pos.toGeoPoint() // Left top corner of the tile
    val end = pos.copy(x = pos.x + 1, y = pos.y + 1).toGeoPoint() // Right bottom corner of the tile
    val meterStart = start.toMeter()
    val meterEnd = end.toMeter()
    val startx = min(meterEnd.x, meterStart.x)
    val starty = min(meterEnd.y, meterStart.y)
    val endx = max(meterStart.x, meterEnd.x)
    val endy = max(meterStart.y, meterEnd.y)

    parameters.append("BBOX", "$startx,$starty,$endx,$endy")
}.build()


private const val mapboxToken = "xxx"//Config.MAPBOX_TOKEN

fun mapbox(tileX: Int, tileY: Int, zoom: Int) =
    "https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/512/$zoom/$tileX/$tileY?access_token=$mapboxToken"

private val client = HttpClient()

class TileProvider(
    val tileSize: Int,
    private val tileLoaderUrl: (TilePos) -> Url,
) {
    val inMemoryCache = mutableMapOf<TilePos, ByteArray>()
    suspend fun loadTile(pos: TilePos) = withContext(Dispatchers.Default) {
        val url = tileLoaderUrl(pos)
        log("TileProvider.loadTile: $url")
        val response = client.request(url)
        response.bodyAsBytes()
            .also { inMemoryCache[pos] = it }
            .decodeToImageBitmap()
    }
    fun cachedTile(pos: TilePos): ImageBitmap? =
        inMemoryCache[pos]?.decodeToImageBitmap()
}