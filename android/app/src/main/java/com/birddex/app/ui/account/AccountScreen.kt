package com.birddex.app.ui.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.birddex.app.BuildConfig
import com.birddex.app.R
import com.birddex.app.ui.components.PlaceholderScreen

/** Sign-in state, settings and feedback. Built in Phases 2 and 5. */
@Composable
fun AccountScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_account),
        message = stringResource(R.string.placeholder_account),
        iconRes = R.drawable.ic_account_circle_outlined,
        modifier = modifier,
        footer = {
            Text(
                "API ${BuildConfig.BASE_URL} · ${BuildConfig.BUILD_TYPE} ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
