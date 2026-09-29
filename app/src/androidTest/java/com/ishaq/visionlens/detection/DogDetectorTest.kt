package com.ishaq.visionlens.detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the full dog pipeline on held-out Stanford Dogs test photos
 * (not used for training) bundled in androidTest/assets/dogs.
 */
@RunWith(AndroidJUnit4::class)
class DogDetectorTest {

    @Test
    fun goldenRetriever() = assertBreed("golden_retriever.jpg", "Golden Retriever")

    @Test
    fun pug() = assertBreed("pug.jpg", "Pug")

    @Test
    fun germanShepherd() = assertBreed("german_shepherd.jpg", "German Shepherd")

    @Test
    fun huskyIsDetectedAsADog() {
        // Siberian Husky, Eskimo Dog and Malamute are near-identical in Stanford Dogs,
        // so only require that the dog is found and given a husky-type label.
        val dog = detectSingleDog(load("siberian_husky.jpg"), rotationDegrees = 0)
        assertTrue(dog.label, dog.label in setOf("Siberian Husky", "Eskimo Dog", "Malamute", "Dog"))
    }

    @Test
    fun rotatedCameraFrameGivesSameBreed() {
        // Camera frames usually arrive rotated 90°; simulate that with a sideways bitmap.
        val upright = load("golden_retriever.jpg")
        val sideways = Bitmap.createBitmap(
            upright, 0, 0, upright.width, upright.height, Matrix().apply { postRotate(-90f) }, true,
        )
        assertEquals("Golden Retriever", detectSingleDog(sideways, rotationDegrees = 90).label)
    }

    private fun assertBreed(file: String, breed: String) {
        assertEquals(breed, detectSingleDog(load(file), rotationDegrees = 0).label)
    }

    private fun detectSingleDog(bitmap: Bitmap, rotationDegrees: Int): Detection {
        val result = detector.detect(bitmap, rotationDegrees, minScore = 0.5f)
        Log.i(TAG, "rotation=$rotationDegrees -> ${result.detections} in ${result.inferenceTimeMs} ms")
        assertEquals("expected exactly one dog", 1, result.detections.size)
        return result.detections.single()
    }

    private fun load(file: String): Bitmap =
        InstrumentationRegistry.getInstrumentation().context.assets.open("dogs/$file").use {
            BitmapFactory.decodeStream(it)
        }

    companion object {
        private const val TAG = "DogDetectorTest"
        private lateinit var detector: DogDetector

        @BeforeClass
        @JvmStatic
        fun setUp() {
            detector = DogDetector(InstrumentationRegistry.getInstrumentation().targetContext)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() = detector.close()
    }
}
