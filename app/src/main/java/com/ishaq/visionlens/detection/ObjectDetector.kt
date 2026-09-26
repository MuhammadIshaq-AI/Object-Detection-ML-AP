package com.ishaq.visionlens.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.io.Closeable

/**
 * Runs EfficientDet-Lite0 (COCO) on-device with the TensorFlow Lite / LiteRT interpreter.
 *
 * Model outputs (in order): boxes [1,N,4] as normalized (ymin, xmin, ymax, xmax),
 * classes [1,N], scores [1,N], count [1].
 */
class ObjectDetector(
    context: Context,
    private val maxResults: Int = 10,
) : Closeable {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputWidth: Int
    private val inputHeight: Int
    private val inputType: DataType

    private val boxes: Array<Array<FloatArray>>
    private val classes: Array<FloatArray>
    private val scores: Array<FloatArray>
    private val count = FloatArray(1)

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_FILE)
        interpreter = Interpreter(model, Interpreter.Options().setNumThreads(NUM_THREADS))
        labels = FileUtil.loadLabels(context, LABEL_FILE)

        val input = interpreter.getInputTensor(0)
        inputHeight = input.shape()[1]
        inputWidth = input.shape()[2]
        inputType = input.dataType()

        val maxDetections = interpreter.getOutputTensor(0).shape()[1]
        boxes = Array(1) { Array(maxDetections) { FloatArray(4) } }
        classes = Array(1) { FloatArray(maxDetections) }
        scores = Array(1) { FloatArray(maxDetections) }
    }

    @Synchronized
    fun detect(bitmap: Bitmap, rotationDegrees: Int, minScore: Float): DetectionResult {
        val processor = ImageProcessor.Builder()
            .add(Rot90Op(-rotationDegrees / 90))
            .add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val image = processor.process(TensorImage(inputType).apply { load(bitmap) })

        val outputs = mapOf<Int, Any>(0 to boxes, 1 to classes, 2 to scores, 3 to count)
        val start = SystemClock.uptimeMillis()
        interpreter.runForMultipleInputsOutputs(arrayOf(image.buffer), outputs)
        val inferenceTime = SystemClock.uptimeMillis() - start

        val found = count[0].toInt().coerceIn(0, scores[0].size)
        val detections = (0 until found)
            .filter { scores[0][it] >= minScore }
            .mapNotNull { i ->
                val classIndex = classes[0][i].toInt()
                val label = labels.getOrNull(classIndex)
                if (label == null || label == UNKNOWN_LABEL) return@mapNotNull null
                val (top, left, bottom, right) = boxes[0][i].map { it.coerceIn(0f, 1f) }
                Detection(label, classIndex, scores[0][i], RectF(left, top, right, bottom))
            }
            .sortedByDescending { it.score }
            .take(maxResults)

        val upright = rotationDegrees % 180 == 0
        return DetectionResult(
            detections = detections,
            inferenceTimeMs = inferenceTime,
            imageWidth = if (upright) bitmap.width else bitmap.height,
            imageHeight = if (upright) bitmap.height else bitmap.width,
        )
    }

    override fun close() = interpreter.close()

    private companion object {
        const val MODEL_FILE = "efficientdet_lite0.tflite"
        const val LABEL_FILE = "labelmap.txt"
        const val UNKNOWN_LABEL = "???"
        const val NUM_THREADS = 4
    }
}
