package com.birddex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.birddex.app.R

/**
 * A user's sighting photo. Cached under [imageKey] (the stable S3 key), not [imageUrl]: the
 * presigned URL changes on every API fetch and expires after an hour, so caching by URL would
 * re-download every time and show nothing offline. Falls back to [BirdPlaceholder].
 */
@Composable
fun SightingPhoto(
    imageKey: String,
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val request = ImageRequest.Builder(LocalPlatformContext.current)
        .data(imageUrl)
        .memoryCacheKey(imageKey)
        .diskCacheKey(imageKey)
        .crossfade(true)
        .build()
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        loading = { BirdPlaceholder() },
        error = { BirdPlaceholder() },
        modifier = modifier,
    )
}

/** Faded origami bird on a grey tile, like the website's "not found yet" cards. */
@Composable
fun BirdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_bird_origami),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .alpha(0.3f),
        )
    }
}
