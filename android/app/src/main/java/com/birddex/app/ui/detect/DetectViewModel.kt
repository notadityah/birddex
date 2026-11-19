package com.birddex.app.ui.detect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.birddex.app.data.detect.DetectPhotoStore
import com.birddex.app.data.detect.DetectRepository
import com.birddex.app.data.detect.Detection
import com.birddex.app.data.detect.ImportResult
import com.birddex.app.data.detect.PhotoPreparer
import com.birddex.app.data.detect.PreparedPhoto
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a photo couldn't be used; shown under the Take / Choose buttons. */
enum class PhotoInputError { NotAnImage, TooLarge, ImportFailed, NoCameraApp, CameraFailed, Unreadable }

data class DetectUiState(
    /** The photo to identify (a private copy), or null before one is chosen. */
    val photo: File? = null,
    /** Copying a picked photo into the cache. */
    val importing: Boolean = false,
    val inputError: PhotoInputError? = null,
    /** Turning [photo] into the JPEGs that get sent; ready when [prepared] is set. */
    val preparing: Boolean = false,
    val prepared: PreparedPhoto? = null,
    /** Waiting for `POST /api/detect`. */
    val detecting: Boolean = false,
    /** The server's answer for [photo]; null until detected. */
    val detection: Detection? = null,
    /** Why the last detect call failed; cleared by retrying. */
    val detectError: ApiError? = null,
) {
    /** "Detect bird" is shown once the photo is ready and hasn't been identified yet. */
    val canDetect: Boolean get() = prepared != null && !detecting && detection == null
}

/**
 * The Detect tab (website: DetectPage.vue). A4.1 covers getting the photo in, from the
 * system photo picker or the system camera; neither needs a permission. A4.2 then prepares
 * it (upright, shrunk, JPEG) as soon as it is chosen, like the website. A4.3 sends it to
 * the model; changing the photo cancels a call that is still running (the website aborts it).
 *
 * Both file paths live in [SavedStateHandle], so the chosen photo, and a camera photo that
 * is still being taken, survive the app being killed in the background (camera apps often
 * cause that on phones with little memory). The prepared JPEGs are only kept in memory and
 * are made again from the saved photo after a restart; so is the detection result, which
 * means asking again.
 */
@HiltViewModel
class DetectViewModel @Inject constructor(
    private val store: DetectPhotoStore,
    private val preparer: PhotoPreparer,
    private val detector: DetectRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(DetectUiState(photo = savedFile(KEY_PHOTO)))
    val state: StateFlow<DetectUiState> = _state.asStateFlow()

    private var prepareJob: Job? = null
    private var detectJob: Job? = null

    init {
        // Drop leftovers from earlier visits, but keep a camera photo that is still on its way.
        store.deleteAllExcept(setOfNotNull(_state.value.photo, savedFile(KEY_PENDING_CAMERA)))
        _state.value.photo?.let(::prepare)
    }

    /** Before launching the camera: the content:// Uri it should write to, or null on failure. */
    fun prepareCamera(): String? {
        _state.update { it.copy(inputError = null) }
        val target = runCatching { store.newCameraTarget() }.getOrNull()
        if (target == null) {
            _state.update { it.copy(inputError = PhotoInputError.CameraFailed) }
            return null
        }
        savedState[KEY_PENDING_CAMERA] = target.file.path
        return target.uri
    }

    /** The system camera has closed; [saved] is its result (false when the user backed out). */
    fun onCameraResult(saved: Boolean) {
        val file = savedFile(KEY_PENDING_CAMERA) ?: return
        savedState.remove<String>(KEY_PENDING_CAMERA)
        if (saved && file.length() > 0) {
            usePhoto(file)
        } else {
            store.delete(file)
            // Some camera apps report success without writing anything.
            if (saved) _state.update { it.copy(inputError = PhotoInputError.CameraFailed) }
        }
    }

    /** No app could take the picture. */
    fun onCameraUnavailable() {
        savedFile(KEY_PENDING_CAMERA)?.let(store::delete)
        savedState.remove<String>(KEY_PENDING_CAMERA)
        _state.update { it.copy(inputError = PhotoInputError.NoCameraApp) }
    }

    /** The photo picker has closed; [uri] is null when the user backed out. */
    fun onPhotoPicked(uri: String?) {
        if (uri == null || _state.value.importing) return
        _state.update { it.copy(importing = true, inputError = null) }
        viewModelScope.launch {
            val error = when (val result = store.importPicked(uri)) {
                is ImportResult.Success -> null.also { usePhoto(result.file) }
                ImportResult.NotAnImage -> PhotoInputError.NotAnImage
                ImportResult.TooLarge -> PhotoInputError.TooLarge
                ImportResult.Failed -> PhotoInputError.ImportFailed
            }
            _state.update { it.copy(importing = false, inputError = error) }
        }
    }

    /** "Detect bird", and "Retry" after a failure. */
    fun detect() {
        val current = _state.value
        val jpeg = current.prepared?.detectJpeg ?: return
        if (current.detecting) return
        _state.update { it.copy(detecting = true, detection = null, detectError = null) }
        detectJob = viewModelScope.launch {
            val result = detector.detect(jpeg)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(detecting = false, detection = result.value)
                    is ApiResult.Failure -> it.copy(detecting = false, detectError = result.error)
                }
            }
        }
    }

    /** "Change photo": back to the Take / Choose buttons, abandoning any detection. */
    fun clearPhoto() {
        prepareJob?.cancel()
        detectJob?.cancel()
        _state.value.photo?.let(store::delete)
        savedState.remove<String>(KEY_PHOTO)
        _state.update { DetectUiState(importing = it.importing) }
    }

    private fun usePhoto(file: File) {
        detectJob?.cancel() // its answer would be for the old photo
        _state.value.photo?.takeIf { it != file }?.let(store::delete)
        savedState[KEY_PHOTO] = file.path
        _state.update {
            it.copy(photo = file, inputError = null, detecting = false, detection = null, detectError = null)
        }
        prepare(file)
    }

    /** Website: resizeImage at 1280 px (detect) and 2048 px (upload), both started on selection. */
    private fun prepare(file: File) {
        prepareJob?.cancel()
        _state.update { it.copy(preparing = true, prepared = null) }
        prepareJob = viewModelScope.launch {
            val prepared = preparer.prepare(file)
            if (_state.value.photo != file) return@launch // replaced or cleared meanwhile
            if (prepared != null) {
                _state.update { it.copy(preparing = false, prepared = prepared) }
            } else {
                // Not a photo this phone can decode (website: "Failed to process image").
                store.delete(file)
                savedState.remove<String>(KEY_PHOTO)
                _state.update {
                    it.copy(photo = null, preparing = false, inputError = PhotoInputError.Unreadable)
                }
            }
        }
    }

    /** A saved path, if its file still exists (the system may clear the cache at any time). */
    private fun savedFile(key: String): File? =
        savedState.get<String>(key)?.let(::File)?.takeIf { it.exists() }

    private companion object {
        const val KEY_PHOTO = "detect_photo"
        const val KEY_PENDING_CAMERA = "detect_pending_camera"
    }
}
