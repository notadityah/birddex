package com.birddex.app.data.detect

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The file handling behind the Detect tab's photo; the Android parts are checked on a device. */
class CacheDetectPhotoStoreTest {

    @get:Rule val temp = TemporaryFolder()

    @Test fun copiesAPhotoWithinTheLimit() {
        val bytes = ByteArray(20_000) { it.toByte() } // more than one copy buffer
        val target = temp.newFile()

        assertTrue(CacheDetectPhotoStore.copyLimited(bytes.inputStream(), target, maxBytes = 20_000))
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun stopsCopyingPastTheLimit() {
        val target = temp.newFile()
        assertFalse(CacheDetectPhotoStore.copyLimited(ByteArray(20_001).inputStream(), target, maxBytes = 20_000))
    }

    @Test fun deletesEverythingButTheKeptPhotos() {
        val dir = temp.newFolder("detect")
        val photo = dir.resolve("picked-1.jpg").apply { writeText("x") }
        val pendingCamera = dir.resolve("camera-2.jpg").apply { createNewFile() }
        dir.resolve("camera-0.jpg").writeText("old")
        dir.resolve("picked-0.png").writeText("old")

        CacheDetectPhotoStore.deleteAllExcept(dir, setOf(photo, pendingCamera))

        assertEquals(setOf("picked-1.jpg", "camera-2.jpg"), dir.list()!!.toSet())
    }

    @Test fun deletingFromAMissingFolderIsFine() {
        CacheDetectPhotoStore.deleteAllExcept(temp.root.resolve("missing"), emptySet())
    }

    @Test fun fileExtensionFollowsTheType() {
        assertEquals("jpg", CacheDetectPhotoStore.extensionFor("image/jpeg"))
        assertEquals("png", CacheDetectPhotoStore.extensionFor("IMAGE/PNG"))
        assertEquals("heic", CacheDetectPhotoStore.extensionFor("image/heif"))
        assertEquals("img", CacheDetectPhotoStore.extensionFor("image/x-adobe-dng"))
        assertEquals("img", CacheDetectPhotoStore.extensionFor(null))
    }
}
