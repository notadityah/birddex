package com.birddex.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.BuildConfig
import com.birddex.app.R
import com.birddex.app.ui.account.AccountViewModel.ApiStatus
import com.birddex.app.ui.components.PlaceholderScreen

/** Sign-in state, settings and feedback. Built in Phases 2 and 5. */
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
    PlaceholderScreen(
        title = stringResource(R.string.tab_account),
        message = stringResource(R.string.placeholder_account),
        iconRes = R.drawable.ic_account_circle_outlined,
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
        footer = {
            ApiStatusFooter(apiStatus, onRetry = viewModel::checkApi)
            if (BuildConfig.DEBUG) AuthCheckPanel()
        },
    )
}

@Composable
private fun ApiStatusFooter(status: ApiStatus, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "API ${BuildConfig.BASE_URL} · ${BuildConfig.BUILD_TYPE} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        when (status) {
            ApiStatus.Checking -> Text(
                stringResource(R.string.api_checking),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            is ApiStatus.Reachable -> Text(
                stringResource(R.string.api_reachable, status.birdCount),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
            )
            is ApiStatus.Unreachable -> {
                Text(
                    stringResource(R.string.api_unreachable, status.reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}
