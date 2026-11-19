package com.birddex.app.data.detect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The chosen photo, upright and re-encoded as the website does before sending it
 * (`useImageResize.js`, called from `DetectPage.vue`). Not a data class: arrays compare by
 * reference, so two preparations are never "equal".
 */
class PreparedPhoto(
    /** Sent to `POST /api/detect` as base64: at most 1280 px, JPEG quality 85. */
    val detectJpeg: ByteArray,
    /** Uploaded with a saved sighting: at most 2048 px, JPEG quality 92. */
    val uploadJpeg: ByteArray,
)

interface PhotoPreparer {
    /** Null if the file isn't a photo this phone can decode. */
    suspend fun prepare(photo: File): PreparedPhoto?
}

data class PhotoSize(val width: Int, val height: Int)

/**
 * Decodes the photo once, at a size close to the larger output, then scales, rotates and
 * compresses each output. Browsers apply the camera's EXIF rotation when they draw a photo,
 * but Android's decoder doesn't, so this reads the tag and turns the pixels itself; the
 * JPEGs it writes carry no EXIF (as on the website, which also drops location data).
 */
class BitmapPhotoPreparer @Inject constructor() : PhotoPreparer {

    override suspend fun prepare(photo: File): PreparedPhoto? = withContext(Dispatchers.Default) {
        // Also catches OutOfMemoryError: a photo too big to decode is just "can't read it".
        runCatching { prepareNow(photo) }.getOrNull()
    }

    private fun prepareNow(photo: File): PreparedPhoto? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photo.path, bounds)
        val (width, height) = bounds.outWidth to bounds.outHeight
        if (width <= 0 || height <= 0) return null

        val uploadSize = scaledSize(width, height, UPLOAD_MAX_DIM)
        val detectSize = scaledSize(width, height, DETECT_MAX_DIM)
        val decoded = BitmapFactory.decodeFile(
            photo.path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(width, height, uploadSize)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null

        val orientation = orientationOf(photo)
        try {
            return PreparedPhoto(
                detectJpeg = encode(decoded, detectSize, orientation, DETECT_QUALITY),
                uploadJpeg = encode(decoded, uploadSize, orientation, UPLOAD_QUALITY),
            )
        } finally {
            decoded.recycle()
        }
    }

    private fun encode(source: Bitmap, size: PhotoSize, orientation: Matrix?, quality: Int): ByteArray {
        val made = mutableListOf<Bitmap>() // everything created here, to free afterwards
        fun Bitmap.track(): Bitmap = also { if (it !== source) made += it }
        try {
            var bitmap = Bitmap.createScaledBitmap(source, size.width, size.height, true).track()
            if (orientation != null) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, orientation, true).track()
            }
            if (bitmap.hasAlpha()) bitmap = onWhite(bitmap).track() // JPEG has no transparency
            return ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) { "JPEG encoding failed" }
                out.toByteArray()
            }
        } finally {
            made.distinct().forEach { it.recycle() }
        }
    }

    private fun onWhite(bitmap: Bitmap): Bitmap =
        Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
            setHasAlpha(false)
        }

    /** The turn (and mirror, for selfies) that makes the photo upright; null if it already is. */
    private fun orientationOf(photo: File): Matrix? {
        val exif = try {
            ExifInterface(photo)
        } catch (e: IOException) {
            return null
        }
        val degrees = exif.rotationDegrees
        if (degrees == 0 && !exif.isFlipped) return null
        return Matrix().apply {
            setRotate(degrees.toFloat())
            if (exif.isFlipped) postScale(-1f, 1f)
        }
    }

    companion object {
        const val DETECT_MAX_DIM = 1280
        const val DETECT_QUALITY = 85
        const val UPLOAD_MAX_DIM = 2048
        const val UPLOAD_QUALITY = 92

        /** The website's `scaleDimensions`: fit within [max] × [max], never enlarge. */
        fun scaledSize(width: Int, height: Int, max: Int): PhotoSize {
            if (width <= max && height <= max) return PhotoSize(width, height)
            val ratio = minOf(max.toDouble() / width, max.toDouble() / height)
            // Math.round rounds halves up, like JavaScript's (Kotlin's round() rounds them to even).
            return PhotoSize(Math.round(width * ratio).toInt(), Math.round(height * ratio).toInt())
        }

        /**
         * The largest power of two that shrinks the photo while decoding without going below
         * [target], so a 50-megapixel photo never has to fit in memory at full size.
         */
        fun sampleSizeFor(width: Int, height: Int, target: PhotoSize): Int {
            var sample = 1
            while (width / (sample * 2) >= target.width && height / (sample * 2) >= target.height) sample *= 2
            return sample
        }
    }
}
