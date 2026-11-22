package com.birddex.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Environment
import android.provider.MediaStore
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves a sighting photo to the phone's Pictures/BirdDex folder (the website's "Download
 * image"). The bytes come from Coil's disk cache, keyed by the photo's S3 key, so this works
 * offline once the photo has been shown; otherwise it downloads the photo first.
 *
 * Android 10+ needs no permission. Android 8–9 needs WRITE_EXTERNAL_STORAGE, which the
 * caller must have requested (the manifest limits it to those versions).
 */
class PhotoSaver(private val context: Context) {

    /** Returns true if the photo was saved. */
    suspend fun save(imageKey: String, imageUrl: String?, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = cachedBytes(imageKey) ?: run {
            if (imageUrl == null) return@run null
            // Not cached yet: let Coil download it into its cache under the same key.
            SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context).data(imageUrl).diskCacheKey(imageKey).memoryCacheKey(imageKey).build(),
            )
            cachedBytes(imageKey)
        } ?: return@withContext false

        val mimeType = mimeTypeFor(imageKey)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveWithMediaStore(bytes, fileName, mimeType) else saveToFile(bytes, fileName, mimeType)
        }.getOrDefault(false)
    }

    private fun cachedBytes(key: String): ByteArray? {
        val cache = SingletonImageLoader.get(context).diskCache ?: return null
        return cache.openSnapshot(key)?.use { snapshot -> cache.fileSystem.read(snapshot.data) { readByteArray() } }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
            put(MediaStore.Images.Media.IS_PENDING, 1) // hidden from other apps until written
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return false
        val written = resolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    @Suppress("DEPRECATION") // the only way on Android 8–9
    private fun saveToFile(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return false
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf(mimeType), null)
        return true
    }

    companion object {
        const val FOLDER = "BirdDex"

        /** The API allows jpg, jpeg, png and webp photos (ALLOWED_EXTS in the backend). */
        fun mimeTypeFor(imageKey: String): String = when (imageKey.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        /** e.g. "BirdDex-house_sparrow-2026-08-06.jpg". */
        fun fileName(slug: String, localDate: String, imageKey: String): String =
            "BirdDex-$slug-$localDate.${imageKey.substringAfterLast('.', "jpg").lowercase()}"
    }
}
