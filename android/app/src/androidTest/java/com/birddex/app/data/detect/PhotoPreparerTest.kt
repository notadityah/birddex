package com.birddex.app.data.detect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real decoding, rotation and JPEG encoding, which only work on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class PhotoPreparerTest {

    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "prep-test").apply { mkdirs() }
    private val preparer = BitmapPhotoPreparer()

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    /** A photo whose left half is red and right half blue, so turns and mirrors are visible. */
    private fun redLeftBlueRight(width: Int, height: Int, orientation: Int? = null): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.BLUE)
            drawRect(0f, 0f, width / 2f, height.toFloat(), Paint().apply { color = Color.RED })
        }
        val file = File(dir, "photo-${System.nanoTime()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (orientation != null) {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return file
    }

    private fun decode(jpeg: ByteArray): Bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

    private fun Bitmap.isRedAt(x: Int, y: Int) = getPixel(x, y).let { Color.red(it) > 200 && Color.blue(it) < 60 }
    private fun Bitmap.isBlueAt(x: Int, y: Int) = getPixel(x, y).let { Color.blue(it) > 200 && Color.red(it) < 60 }

    @Test fun phonePhotoIsShrunkToBothSizes() = runTest {
        val prepared = preparer.prepare(redLeftBlueRight(4032, 3024))!!

        with(decode(prepared.detectJpeg)) { assertEquals(1280 to 960, width to height) }
        with(decode(prepared.uploadJpeg)) { assertEquals(2048 to 1536, width to height) }
        // Both are JPEGs (FF D8), whatever the input was.
        assertEquals(listOf(0xFF.toByte(), 0xD8.toByte()), prepared.detectJpeg.take(2))
        assertTrue(prepared.uploadJpeg.size > prepared.detectJpeg.size)
    }

    @Test fun smallPhotoKeepsItsSize() = runTest {
        val prepared = preparer.prepare(redLeftBlueRight(300, 200))!!

        with(decode(prepared.detectJpeg)) { assertEquals(300 to 200, width to height) }
        with(decode(prepared.uploadJpeg)) { assertEquals(300 to 200, width to height) }
    }

    @Test fun sidewaysCameraPhotoIsTurnedUpright() = runTest {
        // Stored landscape, tagged "turn 90° clockwise to view" (phone held upright).
        val prepared = preparer.prepare(redLeftBlueRight(400, 200, ExifInterface.ORIENTATION_ROTATE_90))!!

        with(decode(prepared.detectJpeg)) {
            assertEquals(200 to 400, width to height)
            assertTrue(isRedAt(100, 50)) // the left half is now on top
            assertTrue(isBlueAt(100, 350))
        }
        // The output is already upright, so it carries no rotation tag for anyone to apply twice.
        assertEquals(0, ExifInterface(prepared.detectJpeg.inputStream()).rotationDegrees)
    }

    @Test fun otherWayRoundToo() = runTest {
        val prepared = preparer.prepare(redLeftBlueRight(400, 200, ExifInterface.ORIENTATION_ROTATE_270))!!

        with(decode(prepared.uploadJpeg)) {
            assertEquals(200 to 400, width to height)
            assertTrue(isBlueAt(100, 50))
            assertTrue(isRedAt(100, 350)) // the left half is now at the bottom
        }
    }

    @Test fun mirroredSelfieIsUnmirrored() = runTest {
        val prepared = preparer.prepare(redLeftBlueRight(400, 200, ExifInterface.ORIENTATION_FLIP_HORIZONTAL))!!

        with(decode(prepared.detectJpeg)) {
            assertEquals(400 to 200, width to height)
            assertTrue(isBlueAt(50, 100))
            assertTrue(isRedAt(350, 100))
        }
    }

    @Test fun transparentPngGoesOnWhite() = runTest {
        val file = File(dir, "clear.png")
        file.outputStream().use {
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        val prepared = preparer.prepare(file)
        assertNotNull(prepared)
        val pixel = decode(prepared!!.detectJpeg).getPixel(50, 50)

        assertTrue(Color.red(pixel) > 245 && Color.green(pixel) > 245 && Color.blue(pixel) > 245)
    }

    @Test fun notAPhotoGivesNull() = runTest {
        val file = File(dir, "notes.jpg").apply { writeText("not really a JPEG") }
        assertNull(preparer.prepare(file))
        assertNull(preparer.prepare(File(dir, "missing.jpg")))
    }
}
