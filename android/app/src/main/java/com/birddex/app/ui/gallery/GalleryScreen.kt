package com.birddex.app.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.birddex.app.R
import com.birddex.app.ui.components.PlaceholderScreen

/** Public community feed. Built in Phase 5 (A5.1–A5.2). */
@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_gallery),
        message = stringResource(R.string.placeholder_gallery),
        iconRes = R.drawable.ic_photo_library_outlined,
        modifier = modifier,
    )
}
