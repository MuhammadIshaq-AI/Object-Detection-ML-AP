package com.ishaq.visionlens.detection

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.Closeable

data class BreedPrediction(val breed: String, val classIndex: Int, val score: Float)

/**
 * Classifies a dog crop into one of the 120 Stanford Dogs breeds.
 *
 * The model (see training/train_dog_breeds.py) is MobileNetV3-Large with a softmax head.
 * Input is raw 0..255 RGB float32 [1,224,224,3]; output is probabilities [1,120].
 */
class DogBreedClassifier(context: Context) : Closeable {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val processor: ImageProcessor
    private val output: Array<FloatArray>

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_FILE)
        interpreter = Interpreter(model, Interpreter.Options().setNumThreads(NUM_THREADS))
        labels = FileUtil.loadLabels(context, LABEL_FILE)

        val shape = interpreter.getInputTensor(0).shape()
        processor = ImageProcessor.Builder()
            .add(ResizeOp(shape[1], shape[2], ResizeOp.ResizeMethod.BILINEAR))
            .build()
        output = Array(1) { FloatArray(interpreter.getOutputTensor(0).shape()[1]) }
    }

    @Synchronized
    fun classify(crop: Bitmap): BreedPrediction {
        val image = processor.process(TensorImage(DataType.FLOAT32).apply { load(crop) })
        interpreter.run(image.buffer, output)
        val probs = output[0]
        val best = probs.indices.maxBy { probs[it] }
        return BreedPrediction(labels[best], best, probs[best])
    }

    override fun close() = interpreter.close()

    private companion object {
        const val MODEL_FILE = "dog_breeds.tflite"
        const val LABEL_FILE = "dog_breeds_labels.txt"
        const val NUM_THREADS = 4
    }
}
