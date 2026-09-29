package com.ishaq.visionlens.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ishaq.visionlens.R
import com.ishaq.visionlens.ui.DetectorUiState
import com.ishaq.visionlens.ui.DetectorViewModel
import com.ishaq.visionlens.ui.components.DetectionOverlay
import com.ishaq.visionlens.ui.theme.BrandGradient
import com.ishaq.visionlens.ui.theme.Cyan
import com.ishaq.visionlens.ui.theme.Indigo
import com.ishaq.visionlens.ui.theme.LiveGreen
import com.ishaq.visionlens.ui.theme.PausedAmber
import com.ishaq.visionlens.ui.theme.TextMuted
import com.ishaq.visionlens.ui.theme.colorForClass
import com.ishaq.visionlens.ui.theme.glass
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun CameraScreen(viewModel: DetectorViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
            setImageAnalysisAnalyzer(analysisExecutor, viewModel::analyze)
        }
    }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            analysisExecutor.shutdown()
        }
    }
    LaunchedEffect(state.torchOn) { controller.enableTorch(state.torchOn) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Scrims keep the floating controls legible on bright scenes.
        Box(
            Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))),
        )
        Box(
            Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
        )

        AnimatedVisibility(
            visible = state.detections.isEmpty() && !state.paused && state.error == null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 1.1f),
            modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp),
        ) {
            ViewfinderReticle()
        }

        DetectionOverlay(
            detections = state.detections,
            imageWidth = state.imageWidth,
            imageHeight = state.imageHeight,
            modifier = Modifier.fillMaxSize(),
        )

        TopBar(
            state = state,
            onToggleTorch = viewModel::toggleTorch,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        ResultsPanel(
            state = state,
            onTogglePause = viewModel::togglePause,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Animated corner brackets with a sweeping scan line, shown while searching for dogs. */
@Composable
private fun ViewfinderReticle() {
    val transition = rememberInfiniteTransition(label = "reticle")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(240.dp).scale(breathe)) {
            val stroke = 4.dp.toPx()
            val len = 44.dp.toPx()
            val inset = stroke / 2f
            val l = inset
            val t = inset
            val r = size.width - inset
            val b = size.height - inset
            val brush = Brush.linearGradient(listOf(Indigo, Cyan), Offset.Zero, Offset(size.width, size.height))
            listOf(
                Offset(l, t + len) to Offset(l, t), Offset(l, t) to Offset(l + len, t),
                Offset(r - len, t) to Offset(r, t), Offset(r, t) to Offset(r, t + len),
                Offset(r, b - len) to Offset(r, b), Offset(r, b) to Offset(r - len, b),
                Offset(l + len, b) to Offset(l, b), Offset(l, b) to Offset(l, b - len),
            ).forEach { (start, end) ->
                drawLine(brush, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
            }

            // Scan line with a soft trailing glow.
            val margin = 18.dp.toPx()
            val y = margin + (size.height - margin * 2) * sweep
            val glow = 36.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Cyan.copy(alpha = 0.18f)),
                    startY = y - glow,
                    endY = y,
                ),
                topLeft = Offset(margin, y - glow),
                size = Size(size.width - margin * 2, glow),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Cyan, Color.Transparent),
                    startX = margin,
                    endX = size.width - margin,
                ),
                start = Offset(margin, y),
                end = Offset(size.width - margin, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Pets, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Looking for dogs…", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TopBar(
    state: DetectorUiState,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.glass(CircleShape).padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Column {
                Text("Vision Lens", style = MaterialTheme.typography.titleMedium, lineHeight = 18.sp)
                Text("Dog breed finder", style = MaterialTheme.typography.labelMedium, color = TextMuted, fontSize = 11.sp, lineHeight = 13.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        StatusPill(state)
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = if (state.torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
            contentDescription = "Toggle flash",
            tint = if (state.torchOn) PausedAmber else Color.White,
            highlighted = state.torchOn,
            onClick = onToggleTorch,
        )
    }
}

@Composable
private fun StatusPill(state: DetectorUiState) {
    val blink by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    val dotColor by animateColorAsState(if (state.paused) PausedAmber else LiveGreen, label = "dot")

    Row(
        Modifier.glass(CircleShape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).alpha(if (state.paused) 1f else blink).background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (state.paused) "PAUSED" else "LIVE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = dotColor,
        )
        if (!state.paused && state.fps > 0f) {
            Text(
                "  ${state.fps.roundToInt()} fps",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    highlighted: Boolean = false,
) {
    val ring by animateColorAsState(if (highlighted) tint.copy(alpha = 0.6f) else Color.Transparent, label = "ring")
    Box(
        Modifier.size(44.dp).clip(CircleShape).glass(CircleShape).border(1.5.dp, ring, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private data class BreedGroup(val label: String, val classIndex: Int, val count: Int, val bestScore: Float)

@Composable
private fun ResultsPanel(
    state: DetectorUiState,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = state.detections
        .groupBy { it.label }
        .map { (label, items) -> BreedGroup(label, items.first().classIndex, items.size, items.maxOf { it.score }) }
        .sortedByDescending { it.bestScore }

    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 18.dp),
    ) {
        // Grabber handle.
        Box(
            Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
        )
        Spacer(Modifier.height(14.dp))

        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dogs spotted", style = MaterialTheme.typography.titleLarge)
                    AnimatedVisibility(state.detections.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        Box(
                            Modifier.padding(start = 10.dp).height(24.dp).background(BrandGradient, CircleShape)
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${state.detections.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        state.error != null -> state.error
                        state.paused -> "Detection paused — tap play to resume"
                        groups.isEmpty() -> "Point your camera at a dog"
                        groups.size == 1 -> "Breed identified"
                        else -> "${groups.size} breeds identified"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else TextMuted,
                )
            }
            PauseButton(paused = state.paused, onClick = onTogglePause)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = groups.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "results",
            modifier = Modifier.height(64.dp),
        ) { empty ->
            if (empty) {
                EmptyBreedHint()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(groups, key = { it.label }) { BreedCard(it) }
                }
            }
        }
    }
}

@Composable
private fun PauseButton(paused: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Box(contentAlignment = Alignment.Center) {
        // Soft halo behind the button.
        Box(
            Modifier.size(56.dp).scale(if (paused) 1f else pulse)
                .background(Cyan.copy(alpha = if (paused) 0f else 0.14f), CircleShape),
        )
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(BrandGradient).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (paused) "Resume detection" else "Pause detection",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun BreedCard(group: BreedGroup) {
    val color = colorForClass(group.classIndex)
    Row(
        Modifier
            .height(64.dp)
            .background(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.06f))),
                RoundedCornerShape(20.dp),
            )
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(start = 10.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Pets, contentDescription = null, tint = Color(0xFF0B0F1A), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                group.label.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (group.count > 1) "${group.count} dogs" else "1 dog",
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun EmptyBreedHint() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(64.dp)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).alpha(shimmer).background(Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Pets, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Breeds will appear here", style = MaterialTheme.typography.labelLarge)
            Text("Hold steady with the dog in frame", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}
