package com.ishaq.visionlens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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

/**
 * Draws detection boxes over a camera preview that uses FILL_CENTER scaling.
 */
@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Midnight, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val pawPainter = rememberVectorPainter(Icons.Rounded.Pets)

    Canvas(modifier) {
        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        // Same math as PreviewView.ScaleType.FILL_CENTER: scale to cover, then center-crop.
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        detections.forEach { detection ->
            val box = detection.box
            val rect = Rect(
                left = box.left * imageWidth * scale + offsetX,
                top = box.top * imageHeight * scale + offsetY,
                right = box.right * imageWidth * scale + offsetX,
                bottom = box.bottom * imageHeight * scale + offsetY,
            )
            val color = colorForClass(detection.classIndex)
            drawBox(rect, color)

            val layout = textMeasurer.measure(detection.label.replaceFirstChar { it.uppercase() }, labelStyle)
            val padH = 10.dp.toPx()
            val padV = 6.dp.toPx()
            val iconSize = 16.dp.toPx()
            val iconGap = 6.dp.toPx()
            val pill = Size(
                padH + iconSize + iconGap + layout.size.width + padH,
                max(layout.size.height.toFloat(), iconSize) + padV * 2,
            )
            val pillX = rect.left.coerceIn(0f, max(0f, size.width - pill.width))
            // Place the label above the box, or inside it if there's no room at the top.
            val gap = 6.dp.toPx()
            val pillY = if (rect.top - pill.height - gap > 0f) rect.top - pill.height - gap else rect.top + gap

            // Drop shadow, then the pill itself.
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(pillX, pillY + 2.dp.toPx()),
                size = pill,
                cornerRadius = CornerRadius(pill.height / 2f),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(pillX, pillY),
                size = pill,
                cornerRadius = CornerRadius(pill.height / 2f),
            )
            translate(pillX + padH, pillY + (pill.height - iconSize) / 2f) {
                with(pawPainter) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Midnight)) }
            }
            drawText(
                layout,
                topLeft = Offset(pillX + padH + iconSize + iconGap, pillY + (pill.height - layout.size.height) / 2f),
            )
        }
    }
}

private fun DrawScope.drawBox(rect: Rect, color: Color) {
    val radius = CornerRadius(16.dp.toPx())
    drawRoundRect(color.copy(alpha = 0.10f), rect.topLeft, rect.size, radius)
    // Soft outer glow followed by a crisp hairline.
    drawRoundRect(color.copy(alpha = 0.18f), rect.topLeft, rect.size, radius, style = Stroke(8.dp.toPx()))
    drawRoundRect(color.copy(alpha = 0.9f), rect.topLeft, rect.size, radius, style = Stroke(1.5.dp.toPx()))

    // Thick corner accents for a "viewfinder" look.
    val len = min(26.dp.toPx(), min(rect.width, rect.height) / 3f)
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
        drawLine(Color.White, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
