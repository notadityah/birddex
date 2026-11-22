package com.birddex.app.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.birddex.app.R
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.gallery.GallerySighting
import com.birddex.app.ui.components.BirdPlaceholder
import com.birddex.app.ui.components.RelativeDate
import com.birddex.app.ui.components.SightingPhoto
import com.birddex.app.ui.components.relativeDate
import java.time.Instant

/**
 * Community Gallery (website: GalleryPage.vue): every shared sighting, newest first, as photo
 * cards with the bird, who shared it and when. More load as you scroll, where the website has
 * a "Load More" button. A species filter narrows the feed, and tapping a card opens it
 * full-screen. Needs a connection; nothing is saved for offline use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val items = viewModel.feed.collectAsLazyPagingItems()
    val refresh = items.loadState.refresh
    val now = Instant.now()
    val birdFilter by viewModel.birdFilter.collectAsStateWithLifecycle()
    val birds by viewModel.birdOptions.collectAsStateWithLifecycle()
    val openSighting by viewModel.openSighting.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // A new filter is a new list: start it from the top. Skipped on first composition, so
    // coming back to the tab keeps the scroll position.
    var shownFilter by rememberSaveable { mutableStateOf(birdFilter) }
    LaunchedEffect(birdFilter) {
        if (birdFilter != shownFilter) {
            shownFilter = birdFilter
            gridState.scrollToItem(0)
        }
    }

    openSighting?.let { GallerySightingDetail(it, onClose = viewModel::close) }

    PullToRefreshBox(
        isRefreshing = refresh is LoadState.Loading && items.itemCount > 0,
        onRefresh = items::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            // Two columns on phones, more on wider screens (website: 2 → 5 columns).
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth { Header() }
            fullWidth { BirdFilter(birds, selected = birdFilter, onSelect = viewModel::setBirdFilter) }
            when {
                refresh is LoadState.Error && items.itemCount == 0 ->
                    fullWidth { Problem(R.string.gallery_load_failed, onRetry = items::retry) }
                refresh is LoadState.Error ->
                    fullWidth { Problem(R.string.gallery_refresh_failed, onRetry = items::retry) }
                refresh is LoadState.Loading && items.itemCount == 0 ->
                    fullWidth { Loading(Modifier.padding(vertical = 48.dp)) }
                refresh is LoadState.NotLoading && items.itemCount == 0 ->
                    fullWidth { Empty(filteredTo = birds.firstOrNull { it.id == birdFilter }) }
            }
            items(count = items.itemCount, key = items.itemKey { it.id }, contentType = items.itemContentType { "sighting" }) { index ->
                items[index]?.let { sighting -> GalleryCard(sighting, now, onClick = { viewModel.open(sighting) }) }
            }
            appendState(items)
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.appendState(items: LazyPagingItems<GallerySighting>) {
    when (items.loadState.append) {
        is LoadState.Loading -> fullWidth { Loading(Modifier.padding(vertical = 16.dp)) }
        is LoadState.Error -> fullWidth { Problem(R.string.gallery_load_more_failed, onRetry = items::retry) }
        is LoadState.NotLoading -> Unit
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(stringResource(R.string.gallery_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
        Text(stringResource(R.string.gallery_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * "All birds" or one species (website: the "Filter by bird" select). The menu lists the birds
 * the app has saved, so it works as soon as the Dex has loaded once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirdFilter(birds: List<Bird>, selected: Int?, onSelect: (Int?) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val allBirds = stringResource(R.string.gallery_all_birds)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = birds.firstOrNull { it.id == selected }?.name ?: allBirds,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.gallery_filter_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allBirds) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            birds.forEach { bird ->
                DropdownMenuItem(
                    text = { Text(bird.name) },
                    onClick = {
                        onSelect(bird.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** One shared sighting (website: GallerySightingCard.vue); tapping it opens the full view. */
@Composable
private fun GalleryCard(sighting: GallerySighting, now: Instant, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            if (sighting.imageUrl != null) {
                SightingPhoto(
                    imageKey = sighting.photoCacheKey,
                    imageUrl = sighting.imageUrl,
                    contentDescription = sighting.birdName,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BirdPlaceholder()
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                sighting.birdName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.gallery_by, sighting.userName ?: stringResource(R.string.gallery_anonymous)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            sighting.sharedAt?.let {
                Text(
                    relativeDateText(relativeDate(it, now)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun relativeDateText(date: RelativeDate): String = when (date) {
    RelativeDate.Today -> stringResource(R.string.date_today)
    RelativeDate.Yesterday -> stringResource(R.string.date_yesterday)
    is RelativeDate.DaysAgo -> pluralStringResource(R.plurals.date_days_ago, date.days.toInt(), date.days)
    is RelativeDate.WeeksAgo -> stringResource(R.string.date_weeks_ago, date.weeks)
    is RelativeDate.MonthsAgo -> stringResource(R.string.date_months_ago, date.months)
    is RelativeDate.YearsAgo -> stringResource(R.string.date_years_ago, date.years)
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(32.dp))
    }
}

@Composable
private fun Empty(filteredTo: Bird?) {
    Text(
        if (filteredTo == null) {
            stringResource(R.string.gallery_empty)
        } else {
            stringResource(R.string.gallery_empty_filtered, filteredTo.name)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    )
}

@Composable
private fun Problem(message: Int, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
