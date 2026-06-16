package de.drick.compose.tilemap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val scaleSteps = listOf(
    1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000,
    20000, 50000, 100000, 200000, 500000, 1000000, 2000000
)

fun DrawScope.drawScaleBar(
    metersPerPixel: Double,
    canvasSize: Size,
    textMeasurer: TextMeasurer,
    textColor: Color
) {
    val maxBarWidthPx = canvasSize.width / 2f
    val maxBarMeters = maxBarWidthPx * metersPerPixel
    val scaleMeters = scaleSteps.lastOrNull { it <= maxBarMeters } ?: scaleSteps.first()
    val barWidthPx = (scaleMeters / metersPerPixel).toFloat()

    val label = if (scaleMeters >= 1000) "${scaleMeters / 1000} km" else "$scaleMeters m"

    val margin = 16f
    val tickHeight = 8f
    val barY = canvasSize.height - margin - tickHeight
    val barStartX = margin
    val barEndX = margin + barWidthPx

    // White outline for contrast
    val outlineColor = Color.White
    val barColor = Color.Black
    val strokeOutline = 4f
    val strokeBar = 2f

    // Draw outline
    drawLine(
        color = outlineColor,
        start = Offset(barStartX, barY),
        end = Offset(barEndX, barY),
        strokeWidth = strokeOutline + strokeBar
    )
    drawLine(
        color = outlineColor,
        start = Offset(barStartX, barY - tickHeight / 2),
        end = Offset(barStartX, barY + tickHeight / 2),
        strokeWidth = strokeOutline + strokeBar
    )
    drawLine(
        color = outlineColor,
        start = Offset(barEndX, barY - tickHeight / 2),
        end = Offset(barEndX, barY + tickHeight / 2),
        strokeWidth = strokeOutline + strokeBar
    )

    // Draw bar
    drawLine(barColor, Offset(barStartX, barY), Offset(barEndX, barY), strokeWidth = strokeBar)
    drawLine(
        color = barColor,
        start = Offset(barStartX, barY - tickHeight / 2),
        end = Offset(barStartX, barY + tickHeight / 2),
        strokeWidth = strokeBar
    )
    drawLine(
        color = barColor,
        start = Offset(barEndX, barY - tickHeight / 2),
        end = Offset(barEndX, barY + tickHeight / 2),
        strokeWidth = strokeBar
    )

    // Draw label
    val textLayoutResult = textMeasurer.measure(label, TextStyle(color = barColor, fontSize = 12.sp))
    val textX = barStartX + (barWidthPx - textLayoutResult.size.width) / 2
    val textY = barY - tickHeight / 2 - textLayoutResult.size.height - 2f
    // White background for text
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(
            color = outlineColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(color = barColor, fontSize = 16.sp)
    )
}
