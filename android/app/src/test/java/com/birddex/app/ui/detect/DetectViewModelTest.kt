package com.birddex.app.ui.detect

import androidx.lifecycle.SavedStateHandle
import com.birddex.app.data.detect.CacheDetectPhotoStore
import com.birddex.app.data.detect.CameraTarget
import com.birddex.app.data.detect.DetectPhotoStore
import com.birddex.app.data.detect.ImportResult
import com.birddex.app.data.detect.PhotoPreparer
import com.birddex.app.data.detect.PreparedPhoto
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DetectViewModelTest {

    @get:Rule val temp = TemporaryFolder()

    /** Real files in a temp folder; only the Uri handling is faked. */
    private inner class FakeDetectPhotoStore : DetectPhotoStore {
        val dir: File by lazy { temp.newFolder("detect") }
        private var counter = 0
        var importResult: (() -> ImportResult)? = null
        var importGate: CompletableDeferred<Unit>? = null
        val imported = mutableListOf<String>()

        override fun newCameraTarget(): CameraTarget {
            val file = dir.resolve("camera-${counter++}.jpg").apply { createNewFile() }
            return CameraTarget(file, "content://photos/${file.name}")
        }

        override suspend fun importPicked(uri: String): ImportResult {
            imported += uri
            importGate?.await()
            return importResult?.invoke()
                ?: ImportResult.Success(dir.resolve("picked-${counter++}.jpg").apply { writeText("photo") })
        }

        override fun delete(file: File) {
            file.delete()
        }

        override fun deleteAllExcept(keep: Set<File>) = CacheDetectPhotoStore.deleteAllExcept(dir, keep)
    }

    /** "Prepares" a photo into its own file name, so tests can tell which photo a result is for. */
    private class FakePhotoPreparer : PhotoPreparer {
        val prepared = mutableListOf<String>()
        var unreadable = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun prepare(photo: File): PreparedPhoto? {
            prepared += photo.name
            gate?.await()
            return if (unreadable) null else PreparedPhoto(photo.name.toByteArray(), "${photo.name}-upload".toByteArray())
        }
    }

    private val store = FakeDetectPhotoStore()
    private val preparer = FakePhotoPreparer()
    private val savedState = SavedStateHandle()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun open() = DetectViewModel(store, preparer, savedState)

    /** Which photo the prepared JPEGs were made from (see FakePhotoPreparer). */
    private fun DetectViewModel.preparedFrom(): String? = state.value.prepared?.detectJpeg?.decodeToString()

    /** What the camera app does: writes the photo into the file behind the Uri. */
    private fun cameraWrites(uri: String) = store.dir.resolve(uri.substringAfterLast('/')).writeText("jpeg")

    // --- camera ---

    @Test fun takenPhotoIsUsed() {
        val vm = open()
        val uri = vm.prepareCamera()!!
        assertEquals("content://photos/camera-0.jpg", uri)

        cameraWrites(uri)
        vm.onCameraResult(saved = true)

        assertEquals("camera-0.jpg", vm.state.value.photo?.name)
        assertNull(vm.state.value.inputError)
    }

    @Test fun backingOutOfTheCameraLeavesNoFile() {
        val vm = open()
        vm.prepareCamera()

        vm.onCameraResult(saved = false)

        assertNull(vm.state.value.photo)
        assertNull(vm.state.value.inputError) // backing out isn't an error
        assertTrue(store.dir.list()!!.isEmpty())
    }

    @Test fun cameraThatSavesNothingIsReported() {
        val vm = open()
        vm.prepareCamera()

        vm.onCameraResult(saved = true) // "OK", but the file is still empty

        assertNull(vm.state.value.photo)
        assertEquals(PhotoInputError.CameraFailed, vm.state.value.inputError)
        assertTrue(store.dir.list()!!.isEmpty())
    }

    @Test fun photoTakenWhileTheAppWasKilledIsStillUsed() {
        val uri = open().prepareCamera()!!
        cameraWrites(uri)

        // The app was killed while the camera was open; it comes back with only its saved state.
        val restored = open()
        assertTrue(store.dir.resolve("camera-0.jpg").exists()) // not cleaned up as a leftover
        restored.onCameraResult(saved = true)

        assertEquals("camera-0.jpg", restored.state.value.photo?.name)
    }

    @Test fun noCameraAppIsReported() {
        val vm = open()
        vm.prepareCamera()

        vm.onCameraUnavailable()

        assertEquals(PhotoInputError.NoCameraApp, vm.state.value.inputError)
        assertTrue(store.dir.list()!!.isEmpty())
        vm.onCameraResult(saved = true) // a stray result afterwards does nothing
        assertNull(vm.state.value.photo)
    }

    // --- photo picker ---

    @Test fun pickedPhotoIsCopiedAndUsed() {
        val vm = open()

        vm.onPhotoPicked("content://media/picker/0/42")

        assertEquals(listOf("content://media/picker/0/42"), store.imported)
        assertEquals("picked-0.jpg", vm.state.value.photo?.name)
        assertFalse(vm.state.value.importing)
    }

    @Test fun backingOutOfThePickerChangesNothing() {
        val vm = open()
        vm.onPhotoPicked(null)

        assertTrue(store.imported.isEmpty())
        assertEquals(DetectUiState(), vm.state.value)
        assertTrue(preparer.prepared.isEmpty())
    }

    @Test fun importErrorsAreReported() {
        val vm = open()
        val cases = mapOf(
            ImportResult.NotAnImage to PhotoInputError.NotAnImage,
            ImportResult.TooLarge to PhotoInputError.TooLarge,
            ImportResult.Failed to PhotoInputError.ImportFailed,
        )
        for ((result, error) in cases) {
            store.importResult = { result }
            vm.onPhotoPicked("content://media/picker/0/1")
            assertEquals(error, vm.state.value.inputError)
            assertNull(vm.state.value.photo)
        }
    }

    @Test fun showsProgressWhileCopyingAndIgnoresASecondPick() = runTest {
        val gate = CompletableDeferred<Unit>()
        store.importGate = gate
        val vm = open()

        vm.onPhotoPicked("content://media/picker/0/1")
        assertTrue(vm.state.value.importing)
        vm.onPhotoPicked("content://media/picker/0/2")

        gate.complete(Unit)
        assertEquals(1, store.imported.size)
        assertFalse(vm.state.value.importing)
    }

    @Test fun aNewErrorClearsWhenThePhotoWorks() {
        val vm = open()
        store.importResult = { ImportResult.Failed }
        vm.onPhotoPicked("content://media/picker/0/1")

        store.importResult = null
        vm.onPhotoPicked("content://media/picker/0/2")

        assertNull(vm.state.value.inputError)
        assertEquals("picked-0.jpg", vm.state.value.photo?.name)
    }

    // --- replacing, clearing, restoring ---

    @Test fun newPhotoReplacesAndDeletesTheOldOne() {
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")
        val first = vm.state.value.photo!!

        val uri = vm.prepareCamera()!!
        cameraWrites(uri)
        vm.onCameraResult(saved = true)

        assertFalse(first.exists())
        assertEquals(listOf("camera-1.jpg"), store.dir.list()!!.toList())
    }

    @Test fun changePhotoDeletesItAndGoesBackToTheButtons() {
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")
        val photo = vm.state.value.photo!!

        vm.clearPhoto()

        assertNull(vm.state.value.photo)
        assertFalse(photo.exists())
        assertNull(open().state.value.photo) // stays cleared after the app restarts
    }

    @Test fun photoSurvivesTheAppBeingKilled() {
        open().onPhotoPicked("content://media/picker/0/1")

        assertEquals("picked-0.jpg", open().state.value.photo?.name)
    }

    @Test fun photoClearedFromTheCacheIsForgotten() {
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")
        vm.state.value.photo!!.delete() // the system cleared the cache

        assertNull(open().state.value.photo)
    }

    @Test fun leftoversFromEarlierVisitsAreDeleted() {
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")
        store.dir.resolve("camera-old.jpg").writeText("old")
        store.dir.resolve("picked-old.jpg").writeText("old")

        open()

        assertEquals(listOf("picked-0.jpg"), store.dir.list()!!.toList())
    }

    // --- preparing the photo (A4.2) ---

    @Test fun chosenPhotoIsPreparedStraightAway() {
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")

        assertEquals(listOf("picked-0.jpg"), preparer.prepared)
        assertEquals("picked-0.jpg", vm.preparedFrom())
        assertFalse(vm.state.value.preparing)

        val uri = vm.prepareCamera()!!
        cameraWrites(uri)
        vm.onCameraResult(saved = true)
        assertEquals("camera-1.jpg", vm.preparedFrom())
    }

    @Test fun showsPreparingUntilDone() {
        val gate = CompletableDeferred<Unit>()
        preparer.gate = gate
        val vm = open()

        vm.onPhotoPicked("content://media/picker/0/1")
        assertTrue(vm.state.value.preparing)
        assertNull(vm.state.value.prepared)

        gate.complete(Unit)
        assertFalse(vm.state.value.preparing)
        assertEquals("picked-0.jpg", vm.preparedFrom())
    }

    @Test fun restoredPhotoIsPreparedAgain() {
        open().onPhotoPicked("content://media/picker/0/1")
        preparer.prepared.clear()

        val restored = open() // the app was killed; the JPEGs were only in memory

        assertEquals(listOf("picked-0.jpg"), preparer.prepared)
        assertEquals("picked-0.jpg", restored.preparedFrom())
    }

    @Test fun unreadablePhotoIsDroppedAndReported() {
        preparer.unreadable = true
        val vm = open()

        vm.onPhotoPicked("content://media/picker/0/1")

        with(vm.state.value) {
            assertNull(photo)
            assertFalse(preparing)
            assertEquals(PhotoInputError.Unreadable, inputError)
        }
        assertTrue(store.dir.list()!!.isEmpty())
        assertNull(open().state.value.photo) // not restored after a restart either
    }

    @Test fun replacedPhotoNeverShowsTheOldResult() {
        val gate = CompletableDeferred<Unit>()
        preparer.gate = gate
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")

        val uri = vm.prepareCamera()!!
        cameraWrites(uri)
        vm.onCameraResult(saved = true) // a new photo while the first is still being prepared
        gate.complete(Unit)

        assertEquals(listOf("picked-0.jpg", "camera-1.jpg"), preparer.prepared)
        assertEquals("camera-1.jpg", vm.preparedFrom())
    }

    @Test fun changePhotoStopsPreparing() {
        val gate = CompletableDeferred<Unit>()
        preparer.gate = gate
        val vm = open()
        vm.onPhotoPicked("content://media/picker/0/1")

        vm.clearPhoto()
        gate.complete(Unit)

        assertEquals(DetectUiState(), vm.state.value)
    }
}
