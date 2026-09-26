package com.ishaq.visionlens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaq.visionlens.detection.Detection
import com.ishaq.visionlens.ui.theme.Midnight
import com.ishaq.visionlens.ui.theme.colorForClass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws detection boxes over a camera preview that uses FILL_CENTER scaling.
 * Boxes are mirrored horizontally for the front camera to match the mirrored preview.
 */
@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Midnight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier) {
        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        // Same math as PreviewView.ScaleType.FILL_CENTER: scale to cover, then center-crop.
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        detections.forEach { detection ->
            val box = detection.box
            val left = if (mirrored) 1f - box.right else box.left
            val right = if (mirrored) 1f - box.left else box.right
            val rect = Rect(
                left = left * imageWidth * scale + offsetX,
                top = box.top * imageHeight * scale + offsetY,
                right = right * imageWidth * scale + offsetX,
                bottom = box.bottom * imageHeight * scale + offsetY,
            )
            val color = colorForClass(detection.classIndex)
            drawBox(rect, color)

            val text = "${detection.label.replaceFirstChar { it.uppercase() }}  ${(detection.score * 100).roundToInt()}%"
            val layout = textMeasurer.measure(text, labelStyle)
            val padH = 8.dp.toPx()
            val padV = 4.dp.toPx()
            val pill = Size(layout.size.width + padH * 2, layout.size.height + padV * 2)
            val pillX = rect.left.coerceIn(0f, max(0f, size.width - pill.width))
            // Place the label above the box, or inside it if there's no room at the top.
            val pillY = if (rect.top - pill.height - 4.dp.toPx() > 0f) {
                rect.top - pill.height - 4.dp.toPx()
            } else {
                rect.top + 6.dp.toPx()
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(pillX, pillY),
                size = pill,
                cornerRadius = CornerRadius(pill.height / 2f),
            )
            drawText(layout, topLeft = Offset(pillX + padH, pillY + padV))
        }
    }
}

private fun DrawScope.drawBox(rect: Rect, color: Color) {
    val radius = CornerRadius(12.dp.toPx())
    drawRoundRect(color.copy(alpha = 0.12f), rect.topLeft, rect.size, radius)
    drawRoundRect(color.copy(alpha = 0.85f), rect.topLeft, rect.size, radius, style = Stroke(2.dp.toPx()))

    // Thick corner accents for a "viewfinder" look.
    val len = min(22.dp.toPx(), min(rect.width, rect.height) / 3f)
    val stroke = 4.dp.toPx()
    val inset = stroke / 2f
    val l = rect.left + inset
    val t = rect.top + inset
    val r = rect.right - inset
    val b = rect.bottom - inset
    listOf(
        Offset(l, t + len) to Offset(l, t), Offset(l, t) to Offset(l + len, t),
        Offset(r - len, t) to Offset(r, t), Offset(r, t) to Offset(r, t + len),
        Offset(r, b - len) to Offset(r, b), Offset(r, b) to Offset(r - len, b),
        Offset(l + len, b) to Offset(l, b), Offset(l, b) to Offset(l, b - len),
    ).forEach { (start, end) ->
        drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
