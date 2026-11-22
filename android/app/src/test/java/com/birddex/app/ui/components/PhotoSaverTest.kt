package com.birddex.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSaverTest {

    @Test fun mimeTypeFollowsTheS3KeysExtension() {
        assertEquals("image/jpeg", PhotoSaver.mimeTypeFor("images/u1/abc.jpg"))
        assertEquals("image/jpeg", PhotoSaver.mimeTypeFor("images/u1/abc.JPEG"))
        assertEquals("image/png", PhotoSaver.mimeTypeFor("images/u1/abc.png"))
        assertEquals("image/webp", PhotoSaver.mimeTypeFor("images/u1/abc.webp"))
    }

    @Test fun fileNameIsReadableAndKeepsTheExtension() {
        assertEquals(
            "BirdDex-house_sparrow-2026-08-06.jpg",
            PhotoSaver.fileName("house_sparrow", "2026-08-06", "images/u1/0b7e9c1d.jpg"),
        )
        assertEquals(
            "BirdDex-house_sparrow-2026-08-06.png",
            PhotoSaver.fileName("house_sparrow", "2026-08-06", "images/u1/0b7e9c1d.PNG"),
        )
    }
}
