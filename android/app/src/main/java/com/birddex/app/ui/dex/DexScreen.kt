package com.birddex.app.ui.dex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.ui.components.PhotoSaver
import java.time.ZoneId
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birddex.app.R
import com.birddex.app.ui.components.BirdPlaceholder
import com.birddex.app.ui.components.SightingPhoto

/**
 * MyDex (website: MyDexPage.vue + BirdCard.vue): every species as a card, found ones with the
 * user's newest photo, plus progress and All / Found / Not Found filters. Tapping a card opens
 * the bird detail sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScreen(
    /** "Go to Detect to find this bird" in the detail sheet. */
    onFindBird: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DexViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The open detail sheet, by bird id so it survives rotation and refreshes.
    var openBirdId by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val savePhoto = rememberPhotoSaving()

    // Short messages from share/delete. Toasts also show above the open sheet.
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                DexMessage.ShareFailed -> R.string.share_failed
                DexMessage.Deleted -> R.string.sighting_deleted
                DexMessage.DeleteFailed -> R.string.delete_failed
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    state.entries.firstOrNull { it.bird.id == openBirdId }?.let { entry ->
        BirdDetailSheet(
            entry = entry,
            actions = SightingActions(
                busyIds = state.busySightingIds,
                onToggleShared = viewModel::toggleShared,
                onSavePhoto = savePhoto,
                onDelete = viewModel::delete,
            ),
            onDismiss = { openBirdId = null },
            onFindBird = {
                openBirdId = null
                onFindBird()
            },
        )
    }

    Box(modifier.fillMaxSize()) {
        when {
            !state.cacheLoaded -> Unit
            state.entries.isEmpty() && state.refreshError != null ->
                LoadFailed(onRetry = viewModel::refresh, modifier = Modifier.align(Alignment.Center))
            state.entries.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                DexGrid(
                    state,
                    onFilter = viewModel::setFilter,
                    onRetry = viewModel::refresh,
                    onOpenBird = { openBirdId = it },
                )
            }
        }
    }
}

@Composable
private fun DexGrid(
    state: DexUiState,
    onFilter: (DexFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenBird: (birdId: Int) -> Unit,
) {
    LazyVerticalGrid(
        // Two columns on phones, more on wider screens (website: 2 → 5 columns).
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { Header(state) }
        if (state.refreshError != null) fullWidth { RefreshFailedBanner(onRetry) }
        fullWidth { Filters(state.filter, onFilter) }
        val visible = state.visibleEntries
        if (visible.isEmpty()) {
            fullWidth { NothingToShow() }
        } else {
            items(visible, key = { it.bird.id }) { entry -> BirdCard(entry, onClick = { onOpenBird(entry.bird.id) }) }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Header(state: DexUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.dex_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
        state.userName?.let {
            Text(stringResource(R.string.dex_welcome, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            pluralStringResource(R.plurals.dex_found_count, state.totalCount, state.foundCount, state.totalCount),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.foundCount.toFloat() / state.totalCount },
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.dex_percent_complete, state.percent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RefreshFailedBanner(onRetry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dex_showing_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun Filters(selected: DexFilter, onFilter: (DexFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DexFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilter(filter) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                DexFilter.All -> R.string.dex_filter_all
                                DexFilter.Found -> R.string.dex_filter_found
                                DexFilter.NotFound -> R.string.dex_filter_not_found
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun BirdCard(entry: DexEntry, onClick: () -> Unit) {
    // A clickable card is read as one item by screen readers ("King Parrot, Found, …").
    Card(onClick = onClick) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val photo = entry.photo
            if (entry.found && photo?.imageKey != null) {
                SightingPhoto(
                    imageKey = photo.imageKey,
                    imageUrl = photo.imageUrl,
                    contentDescription = null, // the name below describes the card
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BirdPlaceholder()
            }
            if (entry.found) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    Text(
                        stringResource(R.string.dex_found_badge),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.bird.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                entry.bird.scientificName,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NothingToShow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.dex_empty_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.dex_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/**
 * Returns a "save this sighting's photo" action. Asks for storage permission first on
 * Android 8–9 (newer versions don't need it) and reports the result as a toast.
 */
@Composable
private fun rememberPhotoSaving(): (Sighting) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = remember { PhotoSaver(context.applicationContext) }
    var pending by remember { mutableStateOf<Sighting?>(null) }

    fun save(sighting: Sighting) {
        val key = sighting.imageKey ?: return
        val day = sighting.seenAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: "photo"
        scope.launch {
            val ok = saver.save(key, sighting.imageUrl, PhotoSaver.fileName(sighting.slug, day, key))
            Toast.makeText(context, if (ok) R.string.photo_saved else R.string.photo_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val sighting = pending
        pending = null
        if (granted && sighting != null) {
            save(sighting)
        } else {
            Toast.makeText(context, R.string.storage_permission_needed, Toast.LENGTH_SHORT).show()
        }
    }

    return { sighting ->
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pending = sighting
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            save(sighting)
        }
    }
}
