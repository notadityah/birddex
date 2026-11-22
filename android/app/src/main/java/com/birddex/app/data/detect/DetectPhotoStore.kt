package com.birddex.app.data.detect

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where the system camera should write its photo: the file, and the content:// Uri to hand it. */
data class CameraTarget(val file: File, val uri: String)

sealed interface ImportResult {
    data class Success(val file: File) : ImportResult
    data object NotAnImage : ImportResult
    data object TooLarge : ImportResult
    data object Failed : ImportResult
}

/**
 * The photo being identified on the Detect tab, kept as a private copy in the app's cache.
 * Copying a picked photo (rather than keeping the picker's Uri) means it still opens after
 * the app is killed in the background, when the picker's read access is gone.
 *
 * Uris are passed as strings so the Detect view model stays testable on the JVM.
 */
interface DetectPhotoStore {
    /** A new empty file for the system camera to write into. */
    fun newCameraTarget(): CameraTarget

    /** Copies a photo the user picked (a content:// Uri) into the cache. */
    suspend fun importPicked(uri: String): ImportResult

    /** Deletes a photo that is no longer needed (replaced, cancelled or cleared). */
    fun delete(file: File)

    /** Deletes every stored photo except [keep], e.g. ones left over from an earlier visit. */
    fun deleteAllExcept(keep: Set<File>)
}

@Singleton
class CacheDetectPhotoStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DetectPhotoStore {

    /** Must match the cache-path in res/xml/file_paths.xml. */
    private val dir: File get() = File(context.cacheDir, DIR_NAME).apply { mkdirs() }

    private val authority: String get() = "${context.packageName}.photos"

    override fun newCameraTarget(): CameraTarget {
        val file = File(dir, "camera-${System.currentTimeMillis()}.jpg").apply { createNewFile() }
        return CameraTarget(file, FileProvider.getUriForFile(context, authority, file).toString())
    }

    override suspend fun importPicked(uri: String): ImportResult = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        val mimeType = context.contentResolver.getType(source)
        if (mimeType != null && !mimeType.startsWith("image/")) return@withContext ImportResult.NotAnImage

        val target = File(dir, "picked-${System.currentTimeMillis()}.${extensionFor(mimeType)}")
        try {
            val input = context.contentResolver.openInputStream(source) ?: return@withContext ImportResult.Failed
            val copied = input.use { copyLimited(it, target, MAX_BYTES) }
            if (copied) ImportResult.Success(target) else ImportResult.TooLarge.also { target.delete() }
        } catch (e: IOException) {
            target.delete()
            ImportResult.Failed
        } catch (e: SecurityException) { // the picker's read access was revoked
            target.delete()
            ImportResult.Failed
        }
    }

    override fun delete(file: File) {
        file.delete()
    }

    override fun deleteAllExcept(keep: Set<File>) = deleteAllExcept(dir, keep)

    companion object {
        const val DIR_NAME = "detect"

        /**
         * The website allows 20 MB, but photos from 100+ megapixel phone cameras can be bigger.
         * The photo is shrunk before it is sent, so this only guards the cache.
         */
        const val MAX_BYTES = 50L * 1024 * 1024

        /** Copies [input] to [target]; returns false, leaving a partial file, if it passes [maxBytes]. */
        fun copyLimited(input: InputStream, target: File, maxBytes: Long): Boolean {
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) return true
                    total += read
                    if (total > maxBytes) return false
                    output.write(buffer, 0, read)
                }
            }
        }

        fun deleteAllExcept(dir: File, keep: Set<File>) {
            val kept = keep.map { it.absoluteFile }.toSet()
            dir.listFiles()?.filter { it.isFile && it.absoluteFile !in kept }?.forEach { it.delete() }
        }

        /** For readable file names only; the photo is decoded by content, not by extension. */
        fun extensionFor(mimeType: String?): String = when (mimeType?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            "image/gif" -> "gif"
            "image/avif" -> "avif"
            else -> "img"
        }
    }
}
