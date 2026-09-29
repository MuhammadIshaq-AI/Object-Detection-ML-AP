package com.ishaq.visionlens.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    LaunchedEffect(state.frontCamera) {
        controller.cameraSelector =
            if (state.frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
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

        DetectionOverlay(
            detections = state.detections,
            imageWidth = state.imageWidth,
            imageHeight = state.imageHeight,
            mirrored = state.frontCamera,
            modifier = Modifier.fillMaxSize(),
        )

        // Scrims keep the floating controls legible on bright scenes.
        Box(
            Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
        )

        TopBar(
            state = state,
            onToggleTorch = viewModel::toggleTorch,
            onSwitchCamera = {
                val target = if (state.frontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                if (runCatching { controller.hasCamera(target) }.getOrDefault(false)) viewModel.switchCamera()
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        ResultsPanel(
            state = state,
            onThresholdChange = viewModel::setThreshold,
            onTogglePause = viewModel::togglePause,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TopBar(
    state: DetectorUiState,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.glass(CircleShape).padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text("Vision Lens", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.weight(1f))

        StatsPill(state)
        Spacer(Modifier.width(8.dp))
        if (!state.frontCamera) {
            GlassIconButton(
                icon = if (state.torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "Toggle flash",
                tint = if (state.torchOn) PausedAmber else Color.White,
                onClick = onToggleTorch,
            )
            Spacer(Modifier.width(8.dp))
        }
        GlassIconButton(Icons.Rounded.Cameraswitch, "Switch camera", onClick = onSwitchCamera)
    }
}

@Composable
private fun StatsPill(state: DetectorUiState) {
    val blink by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    val dotColor by animateColorAsState(if (state.paused) PausedAmber else LiveGreen, label = "dot")

    Row(
        Modifier.glass(CircleShape).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).alpha(if (state.paused) 1f else blink).background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (state.paused) "Paused" else "${state.fps.roundToInt()} fps · ${state.inferenceTimeMs} ms",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).glass(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private data class ObjectGroup(val label: String, val classIndex: Int, val count: Int, val bestScore: Float)

@Composable
private fun ResultsPanel(
    state: DetectorUiState,
    onThresholdChange: (Float) -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = state.detections
        .groupBy { it.label }
        .map { (label, items) -> ObjectGroup(label, items.first().classIndex, items.size, items.maxOf { it.score }) }
        .sortedByDescending { it.bestScore }

    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .padding(top = 18.dp, bottom = 12.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dogs spotted", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = when {
                        state.error != null -> state.error
                        state.paused -> "Detection paused"
                        groups.isEmpty() -> "Scanning… point at a dog"
                        else -> "${state.detections.size} dog${if (state.detections.size == 1) "" else "s"} in view"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else TextMuted,
                )
            }
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(BrandGradient).clickable(onClick = onTogglePause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (state.paused) "Resume detection" else "Pause detection",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = groups.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "results",
            modifier = Modifier.height(44.dp),
        ) { empty ->
            if (empty) {
                ScanningHint()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.label }) { ObjectChip(it) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Min confidence", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.width(12.dp))
            Slider(
                value = state.threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.2f..0.9f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Cyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${(state.threshold * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(40.dp),
            )
        }
    }
}

@Composable
private fun ObjectChip(group: ObjectGroup) {
    val color = colorForClass(group.classIndex)
    Row(
        Modifier
            .height(40.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(group.label.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
        if (group.count > 1) {
            Spacer(Modifier.width(6.dp))
            Text(
                "×${group.count}",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(group.bestScore * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun ScanningHint() {
    val sweep by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "scanAlpha",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) { i ->
            Box(
                Modifier
                    .weight(1f - i * 0.2f)
                    .height(40.dp)
                    .alpha(sweep * (1f - i * 0.25f))
                    .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp)),
            )
        }
    }
}
