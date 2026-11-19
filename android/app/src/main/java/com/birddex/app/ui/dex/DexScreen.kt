package com.birddex.app.ui.dex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.birddex.app.R
import com.birddex.app.ui.components.PlaceholderScreen

/** Personal dex. Built in Phase 3 (A3.4). */
@Composable
fun DexScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_dex),
        message = stringResource(R.string.placeholder_dex),
        iconRes = R.drawable.ic_grid_view_outlined,
        modifier = modifier,
    )
}
