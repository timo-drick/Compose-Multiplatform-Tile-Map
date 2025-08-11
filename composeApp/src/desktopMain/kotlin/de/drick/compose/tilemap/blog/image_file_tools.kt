package de.drick.compose.tilemap.blog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import de.drick.compose.tilemap.blog.v1.MinimalTileMapView
import de.drick.compose.tilemap.blog.v2.CenteredTileMapView
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

fun main() {
    val outputFolder = File("/home/timo/gitlab/blog/compose_multiplatform_tile_map")
    val image = createImageFromCompose(600, 350) {
        MinimalTileMapView(
            modifier = Modifier.border(width = 1.dp, color = Color.Black).fillMaxSize(),
            zoom = 13,
            start = p1
        )
    }
    saveImage(image, File(outputFolder, "minimal_map.png"), EncodedImageFormat.PNG)

    val image2 = createImageFromCompose(600, 350) {
        CenteredTileMapView(
            modifier = Modifier.border(width = 1.dp, color = Color.Black).fillMaxSize(),
            zoom = 13,
            start = p1
        )
    }
    saveImage(image2, File(outputFolder, "centered_map.png"), EncodedImageFormat.PNG)

    val image3 = createImageFromCompose(600, 350) {
        TileMapViewFill(
            modifier = Modifier.border(width = 1.dp, color = Color.Black).fillMaxSize(),
            zoom = 13,
            start = p1
        )
    }
    saveImage(image3, File(outputFolder, "fill_map.png"), EncodedImageFormat.PNG)

    val image4 = createImageFromCompose(600, 350) {
        TileMapViewFill(
            modifier = Modifier.border(width = 1.dp, color = Color.Black).fillMaxSize(),
            zoom = 13,
            start = p1,
            drawLine = true
        )
    }
    saveImage(image4, File(outputFolder, "full_map.png"), EncodedImageFormat.PNG)
}

fun drawImageToBackground(
    image: ImageBitmap,
    border: Size,
    color: Color
): ImageBitmap {
    val background = ImageBitmap(
        width = image.width + border.width.toInt() * 2,
        height = image.height + border.height.toInt() * 2
    )
    val canvas = Canvas(background)
    val paint = Paint()
    paint.color = color
    paint.style = PaintingStyle.Fill
    canvas.drawRect(
        left = 0f,
        top = 0f,
        right = background.width.toFloat(),
        bottom = background.height.toFloat(),
        paint = paint
    )
    canvas.drawImage(
        topLeftOffset = Offset(border.width, border.height),
        image = image,
        paint = Paint()
    )
    return background
}

fun createImageFromCompose(
    width: Int = 1000,
    height: Int = 1000,
    content: @Composable () -> Unit
): ImageBitmap {
    return ImageComposeScene(
        width = width,
        height = height,
        content = content
    ).use { scene ->
        (0 until 100).forEach { i->
            scene.render(i * 1000L)
            Thread.sleep(100)
        }
        scene.render(100 * 1000).toComposeImageBitmap()
    }
}
fun saveImage(
    image: ImageBitmap,
    toFile: File,
    format: EncodedImageFormat = EncodedImageFormat.WEBP
) {
    val skiaBitmap = Image.makeFromBitmap(image.asSkiaBitmap())
    val data =
        skiaBitmap.encodeToData(format) ?: error("Unable to create webp image")
    toFile.writeBytes(data.bytes)
}
