package com.ishaq.visionlens.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import java.io.Closeable
import kotlin.math.roundToInt

/**
 * Two-stage dog pipeline: EfficientDet (COCO) finds the dogs, then [DogBreedClassifier]
 * names the breed of each one. Detections keep the detector's box, but their label,
 * class index and score come from the breed classifier.
 */
class DogDetector(context: Context) : Closeable {

    private val detector = ObjectDetector(context, allowedLabels = setOf(DOG_LABEL))
    private val classifier = DogBreedClassifier(context)

    fun detect(bitmap: Bitmap, rotationDegrees: Int, minScore: Float): DetectionResult {
        val start = SystemClock.uptimeMillis()
        val upright = if (rotationDegrees == 0) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(rotationDegrees.toFloat()) }, true,
        )

        val result = detector.detect(upright, rotationDegrees = 0, minScore = minScore)
        val dogs = result.detections.map { dog ->
            val breed = classifier.classify(crop(upright, dog.box))
            if (breed.score >= MIN_BREED_SCORE) {
                dog.copy(label = breed.breed, classIndex = breed.classIndex, score = breed.score)
            } else {
                // Not confident about any single breed (mixed breed, odd angle, etc.).
                dog.copy(label = "Dog", classIndex = UNKNOWN_BREED_INDEX)
            }
        }

        return result.copy(detections = dogs, inferenceTimeMs = SystemClock.uptimeMillis() - start)
    }

    /** Crops [box] (normalized) with the same padding the classifier was trained with. */
    private fun crop(image: Bitmap, box: RectF): Bitmap {
        val padX = box.width() * BOX_PADDING
        val padY = box.height() * BOX_PADDING
        val left = ((box.left - padX) * image.width).roundToInt().coerceIn(0, image.width - 1)
        val top = ((box.top - padY) * image.height).roundToInt().coerceIn(0, image.height - 1)
        val right = ((box.right + padX) * image.width).roundToInt().coerceIn(left + 1, image.width)
        val bottom = ((box.bottom + padY) * image.height).roundToInt().coerceIn(top + 1, image.height)
        return Bitmap.createBitmap(image, left, top, right - left, bottom - top)
    }

    override fun close() {
        detector.close()
        classifier.close()
    }

    private companion object {
        const val DOG_LABEL = "dog"
        const val BOX_PADDING = 0.1f
        const val MIN_BREED_SCORE = 0.3f
        const val UNKNOWN_BREED_INDEX = -1
    }
}
