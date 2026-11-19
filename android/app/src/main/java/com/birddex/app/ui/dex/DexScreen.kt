package com.birddex.app.ui.dex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.Rarity

/**
 * The species list, read from the on-device copy and refreshed from the server (A3.2).
 * A3.4 turns this into the dex grid with seen/unseen birds and completion %.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScreen(
    modifier: Modifier = Modifier,
    viewModel: DexViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        when {
            !state.cacheLoaded -> Unit
            state.birds.isEmpty() && state.refreshError != null ->
                LoadFailed(onRetry = viewModel::refresh, modifier = Modifier.align(Alignment.Center))
            state.birds.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                BirdList(state.birds, showingSavedCopy = state.refreshError != null)
            }
        }
    }
}

@Composable
private fun BirdList(birds: List<Bird>, showingSavedCopy: Boolean) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.tab_dex), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    pluralStringResource(R.plurals.dex_species_count, birds.size, birds.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showingSavedCopy) {
                    Text(
                        stringResource(R.string.dex_showing_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        items(birds, key = { it.id }) { bird ->
            BirdRow(bird)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun BirdRow(bird: Bird) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(bird.name, style = MaterialTheme.typography.titleMedium)
            Text(
                bird.scientificName,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RarityBadge(bird.rarityLevel)
    }
}

/** Same colours as the website's badges: grey common, green uncommon, amber rare. */
@Composable
fun RarityBadge(rarity: Rarity, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content, label) = when (rarity) {
        Rarity.Common -> Triple(colors.surfaceVariant, colors.onSurfaceVariant, R.string.rarity_common)
        Rarity.Uncommon -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, R.string.rarity_uncommon)
        Rarity.Rare -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, R.string.rarity_rare)
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.extraLarge, modifier = modifier) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.dex_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
