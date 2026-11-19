package com.birddex.app.ui.detect

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.birddex.app.R
import com.birddex.app.ui.components.PlaceholderScreen

/** Photo → bird detection. Built in Phase 4. */
@Composable
fun DetectScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_detect),
        message = stringResource(R.string.placeholder_detect),
        iconRes = R.drawable.ic_photo_camera_outlined,
        modifier = modifier,
    )
}
