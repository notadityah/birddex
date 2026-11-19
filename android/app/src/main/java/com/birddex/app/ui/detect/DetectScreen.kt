package com.birddex.app.ui.detect

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.birddex.app.R
import java.io.File

/**
 * Photo → bird detection (website: DetectPage.vue + ImageUploader.vue). The photo comes from
 * the system photo picker or the system camera app, so no permission is needed: the picker
 * grants access to just the chosen photo, and the camera app writes into a file we share
 * with it. (Declaring the CAMERA permission would make it required, so the app doesn't.)
 */
@Composable
fun DetectScreen(
    modifier: Modifier = Modifier,
    viewModel: DetectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onPhotoPicked(uri?.toString())
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        viewModel.onCameraResult(saved)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.tab_detect), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.detect_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val photo = state.photo
        if (photo == null) {
            PhotoInput(
                hasCamera = hasCamera,
                importing = state.importing,
                error = state.inputError,
                onTakePhoto = {
                    viewModel.prepareCamera()?.let { uri ->
                        try {
                            takePhoto.launch(Uri.parse(uri))
                        } catch (e: ActivityNotFoundException) {
                            viewModel.onCameraUnavailable()
                        }
                    }
                },
                onChoosePhoto = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        } else {
            SelectedPhoto(photo, onChangePhoto = viewModel::clearPhoto)
        }
    }
}

/** The website's ImageUploader: "Take photo" (when the phone has a camera) and "Choose photo". */
@Composable
private fun PhotoInput(
    hasCamera: Boolean,
    importing: Boolean,
    error: PhotoInputError?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bird_origami),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            if (hasCamera) {
                Button(
                    onClick = onTakePhoto,
                    enabled = !importing,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ButtonIcon(R.drawable.ic_photo_camera_outlined)
                    Text(stringResource(R.string.take_photo))
                }
            }
            OutlinedButton(
                onClick = onChoosePhoto,
                enabled = !importing,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonIcon(R.drawable.ic_photo_library_outlined)
                Text(stringResource(R.string.choose_photo))
            }
            if (importing) CircularProgressIndicator(Modifier.size(24.dp))
            error?.let {
                Text(
                    stringResource(it.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun ButtonIcon(iconRes: Int) {
    Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
}

@Composable
private fun SelectedPhoto(photo: File, onChangePhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = photo,
            contentDescription = stringResource(R.string.detect_selected_photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 384.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        TextButton(onClick = onChangePhoto) { Text(stringResource(R.string.change_photo)) }
    }
}

private fun PhotoInputError.messageRes(): Int = when (this) {
    PhotoInputError.NotAnImage -> R.string.detect_error_not_image
    PhotoInputError.TooLarge -> R.string.detect_error_too_large
    PhotoInputError.ImportFailed -> R.string.detect_error_import
    PhotoInputError.NoCameraApp -> R.string.detect_error_no_camera
    PhotoInputError.CameraFailed -> R.string.detect_error_camera
    PhotoInputError.Unreadable -> R.string.detect_error_unreadable
}
