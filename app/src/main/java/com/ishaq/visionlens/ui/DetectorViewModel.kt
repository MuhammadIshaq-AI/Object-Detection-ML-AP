package com.ishaq.visionlens.ui

import android.app.Application
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import com.ishaq.visionlens.detection.Detection
import com.ishaq.visionlens.detection.DogDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DetectorUiState(
    val detections: List<Detection> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val inferenceTimeMs: Long = 0,
    val fps: Float = 0f,
    val threshold: Float = 0.5f,
    val paused: Boolean = false,
    val frontCamera: Boolean = false,
    val torchOn: Boolean = false,
    val error: String? = null,
)

class DetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DetectorUiState())
    val state: StateFlow<DetectorUiState> = _state.asStateFlow()

    private val detector: DogDetector? = try {
        DogDetector(application)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load model", e)
        _state.update { it.copy(error = "Couldn't load the detection model.") }
        null
    }

    private var lastFrameAt = 0L

    /** Called on the camera analysis thread for every frame. */
    fun analyze(image: ImageProxy) {
        image.use {
            val current = _state.value
            if (detector == null || current.paused) return

            val result = detector.detect(
                bitmap = image.toBitmap(),
                rotationDegrees = image.imageInfo.rotationDegrees,
                minScore = current.threshold,
            )

            val now = System.nanoTime()
            val instantFps = if (lastFrameAt == 0L) 0f else 1e9f / (now - lastFrameAt)
            lastFrameAt = now

            _state.update {
                it.copy(
                    detections = result.detections,
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight,
                    inferenceTimeMs = result.inferenceTimeMs,
                    fps = if (it.fps == 0f) instantFps else it.fps * 0.85f + instantFps * 0.15f,
                )
            }
        }
    }

    fun setThreshold(value: Float) = _state.update { it.copy(threshold = value) }

    fun togglePause() {
        lastFrameAt = 0L
        _state.update { it.copy(paused = !it.paused) }
    }

    fun toggleTorch() = _state.update { it.copy(torchOn = !it.torchOn) }

    fun switchCamera() {
        lastFrameAt = 0L
        _state.update {
            it.copy(frontCamera = !it.frontCamera, torchOn = false, detections = emptyList(), fps = 0f)
        }
    }

    override fun onCleared() {
        detector?.close()
    }

    private companion object {
        const val TAG = "DetectorViewModel"
    }
}
