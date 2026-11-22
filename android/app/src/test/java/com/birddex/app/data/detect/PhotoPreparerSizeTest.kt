package com.birddex.app.data.detect

import com.birddex.app.data.detect.BitmapPhotoPreparer.Companion.sampleSizeFor
import com.birddex.app.data.detect.BitmapPhotoPreparer.Companion.scaledSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** Output sizes, which must match the website's useImageResize.js; decoding is tested on a device. */
class PhotoPreparerSizeTest {

    @Test fun landscapePhoneShotFitsTheLongSide() {
        assertEquals(PhotoSize(1280, 960), scaledSize(4032, 3024, 1280))
        assertEquals(PhotoSize(2048, 1536), scaledSize(4032, 3024, 2048))
    }

    @Test fun portraitFitsTheHeight() {
        assertEquals(PhotoSize(960, 1280), scaledSize(3024, 4032, 1280))
    }

    @Test fun smallPhotoIsNeverEnlarged() {
        assertEquals(PhotoSize(800, 600), scaledSize(800, 600, 1280))
        assertEquals(PhotoSize(1280, 1280), scaledSize(1280, 1280, 1280))
    }

    @Test fun roundsLikeJavaScript() {
        assertEquals(PhotoSize(512, 170), scaledSize(1000, 333, 512)) // 170.496 → 170
        assertEquals(PhotoSize(2, 1), scaledSize(4, 1, 2)) // 0.5 → 1 (half-even rounding would give 0)
    }

    @Test fun decodesAtTheLargestHalvingThatStaysAboveTheTarget() {
        assertEquals(1, sampleSizeFor(4032, 3024, PhotoSize(2048, 1536))) // 2016 would be too small
        assertEquals(2, sampleSizeFor(8160, 6120, PhotoSize(2048, 1536))) // 50 MP: 4080 × 3060
        assertEquals(4, sampleSizeFor(16320, 12240, PhotoSize(2048, 1536))) // 200 MP
        assertEquals(1, sampleSizeFor(800, 600, PhotoSize(800, 600)))
    }
}
