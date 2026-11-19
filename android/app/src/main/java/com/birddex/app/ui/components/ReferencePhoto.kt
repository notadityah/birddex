package com.birddex.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.birddex.app.BuildConfig
import com.birddex.app.R
import com.birddex.app.data.birds.BirdImages

/**
 * A species' reference photo, loaded from the website (`<site>/birds/<slug>.jpg`) and cached
 * by Coil, with [BirdPlaceholder] while loading or if it can't load (the website's fallback).
 * Wherever it is shown, show [PhotoCreditText] too: the photos' licences require it.
 */
@Composable
fun ReferencePhoto(
    slug: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val request = remember(slug) {
        ImageRequest.Builder(context).data(BirdImages.url(BuildConfig.BASE_URL, slug)).crossfade(true).build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        loading = { BirdPlaceholder() },
        error = { BirdPlaceholder() },
        modifier = modifier,
    )
}

/**
 * "Photo: JJ Harrison (CC BY-SA 3.0)" with the photographer linked to the photo's Wikimedia
 * Commons page (website: BirdStatsCard.vue and DetectPage.vue). Nothing if there's no credit.
 */
@Composable
fun PhotoCreditText(slug: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    val credit = BirdImages.credit(slug) ?: return
    val full = stringResource(R.string.photo_credit, credit.author, credit.license)
    val linkColor = MaterialTheme.colorScheme.onSurfaceVariant
    val text = remember(full, credit, linkColor) {
        val start = full.indexOf(credit.author)
        buildAnnotatedString {
            if (start < 0) { // a translation that reworded the author away: show it unlinked
                append(full)
                return@buildAnnotatedString
            }
            append(full.substring(0, start))
            withLink(
                LinkAnnotation.Url(
                    credit.sourceUrl,
                    TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ),
            ) { append(credit.author) }
            append(full.substring(start + credit.author.length))
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}
