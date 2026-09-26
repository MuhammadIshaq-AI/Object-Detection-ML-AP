package com.ishaq.visionlens.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Indigo = Color(0xFF818CF8)
val Cyan = Color(0xFF22D3EE)
val Midnight = Color(0xFF0B0F1A)
val DeepIndigo = Color(0xFF1E1B4B)
val Glass = Color(0xB3101426)
val GlassBorder = Color(0x1FFFFFFF)
val LiveGreen = Color(0xFF34D399)
val PausedAmber = Color(0xFFFBBF24)
val TextMuted = Color(0xFF94A3B8)

val BrandGradient = Brush.linearGradient(listOf(Indigo, Cyan))

/** Vibrant palette used to color each object class consistently. */
private val ClassColors = listOf(
    Color(0xFF22D3EE), Color(0xFFA78BFA), Color(0xFFF472B6), Color(0xFF34D399),
    Color(0xFFFBBF24), Color(0xFFFB7185), Color(0xFF60A5FA), Color(0xFFF97316),
    Color(0xFF2DD4BF), Color(0xFFC084FC),
)

fun colorForClass(classIndex: Int): Color = ClassColors[classIndex.mod(ClassColors.size)]

/** Frosted "glass" surface used for floating controls over the camera feed. */
fun Modifier.glass(shape: Shape): Modifier =
    background(Glass, shape).border(1.dp, GlassBorder, shape)

private val VisionColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Midnight,
    background = Midnight,
    onBackground = Color.White,
    surface = Color(0xFF121729),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E2438),
    onSurfaceVariant = TextMuted,
)

private val VisionTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun VisionLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VisionColors, typography = VisionTypography, content = content)
}
