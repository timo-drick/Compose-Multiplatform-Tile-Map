package de.drick.compose.tilemap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import de.drick.wtf_osd_player.tools.LRUCache
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.get

private val client = HttpClient()
fun osmFree(zoom: Int, tileX: Int, tileY: Int) = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"

private const val mapboxToken = "xxx"//Config.MAPBOX_TOKEN

fun mapbox(zoom: Int, tileX: Int, tileY: Int) =
    "https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/512/$zoom/$tileX/$tileY?access_token=$mapboxToken"


val osmTileProvider: TileProvider = object : TileProvider {
    override val tileSize = 256
    val inMemoryCache = mutableMapOf<String, ByteArray>()
    override suspend fun loadTile(zoom: Int, x: Int, y: Int) = withContext(Dispatchers.Default) {
        val response = client.request(Url(osmFree(zoom, x, y)))
        response.bodyAsBytes()
            .also { inMemoryCache[key(zoom, x, y)] = it }
            .decodeToImageBitmap()
    }
    override fun cachedTile(zoom: Int, x: Int, y: Int): ImageBitmap? =
        inMemoryCache[key(zoom, x, y)]?.decodeToImageBitmap()
    fun key(zoom: Int, x: Int, y: Int) = "$zoom-$x-$y"
}

val mapboxTileProvider: TileProvider = object : TileProvider {
    override val tileSize = 512
    val inMemoryCache = LRUCache<String, ByteArray>(100)
    override suspend fun loadTile(zoom: Int, x: Int, y: Int) = withContext(Dispatchers.Default) {
        // Double check if already cached
        val cached = inMemoryCache[key(zoom, x, y)]
        if (cached != null) {
            return@withContext cached.decodeToImageBitmap()
        }
        val response = client.request(Url(mapbox(zoom, x, y)))
        response.bodyAsBytes()
            .also { inMemoryCache[key(zoom, x, y)] = it }
            .decodeToImageBitmap()
    }
    override fun cachedTile(zoom: Int, x: Int, y: Int): ImageBitmap? =
        inMemoryCache[key(zoom, x, y)]?.decodeToImageBitmap()
    fun key(zoom: Int, x: Int, y: Int) = "$zoom-$x-$y"
}