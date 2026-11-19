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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.network.ApiError
import com.birddex.app.ui.components.BirdStatsCard
import com.birddex.app.ui.components.PhotoCreditText
import com.birddex.app.ui.components.ReferencePhoto
import java.io.File

/**
 * Photo → bird detection (website: DetectPage.vue + ImageUploader.vue). The photo comes from
 * the system photo picker or the system camera app, so no permission is needed: the picker
 * grants access to just the chosen photo, and the camera app writes into a file we share
 * with it. (Declaring the CAMERA permission would make it required, so the app doesn't.)
 */
@Composable
fun DetectScreen(
    /** "View in MyDex" after saving a sighting. */
    onOpenDex: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dex by viewModel.dex.collectAsStateWithLifecycle()
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
        val saved = state.saved
        if (saved != null) {
            SightingSaved(
                bird = saved,
                dex = dex,
                onOpenDex = {
                    viewModel.detectAnother() // the website's Detect page starts over when you come back
                    onOpenDex()
                },
                onDetectAnother = viewModel::detectAnother,
            )
        } else if (photo == null) {
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
            SelectedPhoto(
                photo,
                matchedBird = state.selectedBird,
                // Hidden while saving, as on the website: half a save would leave a photo-less sighting.
                onChangePhoto = viewModel::clearPhoto.takeUnless { state.saving },
            )
            DetectSection(
                state,
                dex = dex,
                onDetect = viewModel::detect,
                onSelectMatch = viewModel::selectMatch,
                onSave = viewModel::save,
            )
        }
    }
}

/** Under the photo (website: the Detect button, spinner, error banner and results). */
@Composable
private fun DetectSection(
    state: DetectUiState,
    dex: DexProgress,
    onDetect: () -> Unit,
    onSelectMatch: (String) -> Unit,
    onSave: (notes: String, shareToGallery: Boolean) -> Unit,
) {
    val error = state.detectError
    val detection = state.detection
    when {
        state.detecting -> Detecting()
        error != null -> DetectFailed(error, onRetry = onDetect)
        detection != null -> {
            DetectionResults(detection, selectedSlug = state.selectedSlug, onSelect = onSelectMatch)
            // The reference photo is already beside the user's (website: show-photo="false").
            state.selectedBird?.let { bird ->
                BirdStatsCard(bird)
                SightingSaver(
                    bird = bird,
                    alreadyFound = bird.id in dex.foundBirdIds,
                    saving = state.saving,
                    error = state.saveError,
                    onSave = onSave,
                )
            }
        }
        else -> Button(onClick = onDetect, enabled = state.canDetect, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.detect_button))
        }
    }
}

@Composable
private fun Detecting() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.detecting), style = MaterialTheme.typography.bodyMedium)
        }
        // The server loads the model on its first request after a quiet spell (10–15 s).
        Text(
            stringResource(R.string.detecting_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetectFailed(error: ApiError, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val message = if (error == ApiError.Network) R.string.detect_offline else R.string.detect_failed
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
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

/**
 * The user's photo, whole; once a bird is matched, side by side with that bird's reference
 * photo instead, so a wrong match is obvious at a glance (website: DetectPage.vue).
 */
@Composable
private fun SelectedPhoto(photo: File, matchedBird: Bird?, onChangePhoto: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (matchedBird == null) {
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
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ComparedPhoto(caption = stringResource(R.string.your_photo), modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = photo,
                        contentDescription = stringResource(R.string.detect_selected_photo),
                        contentScale = ContentScale.Crop,
                        modifier = it,
                    )
                }
                ComparedPhoto(
                    caption = stringResource(R.string.reference_caption, matchedBird.name),
                    credit = { PhotoCreditText(matchedBird.slug, textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f),
                ) {
                    ReferencePhoto(
                        slug = matchedBird.slug,
                        contentDescription = stringResource(R.string.reference_photo_description, matchedBird.name),
                        modifier = it,
                    )
                }
            }
        }
        onChangePhoto?.let { TextButton(onClick = it) { Text(stringResource(R.string.change_photo)) } }
    }
}

@Composable
private fun ComparedPhoto(
    caption: String,
    modifier: Modifier = Modifier,
    credit: @Composable () -> Unit = {},
    image: @Composable (Modifier) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        image(
            Modifier
                .fillMaxWidth()
                .height(192.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        credit()
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
