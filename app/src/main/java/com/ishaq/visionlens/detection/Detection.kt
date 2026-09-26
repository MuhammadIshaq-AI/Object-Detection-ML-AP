package com.ishaq.visionlens.detection

import android.graphics.RectF

/**
 * A single detected object. [box] is normalized to 0..1 in the upright
 * (rotation-corrected) camera frame.
 */
data class Detection(
    val label: String,
    val classIndex: Int,
    val score: Float,
    val box: RectF,
)

data class DetectionResult(
    val detections: List<Detection>,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
)
