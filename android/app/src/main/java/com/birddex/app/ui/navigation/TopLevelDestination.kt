package com.birddex.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.birddex.app.R
import kotlinx.serialization.Serializable

// Type-safe routes for the four tabs.
@Serializable data object DexRoute
@Serializable data object DetectRoute
@Serializable data object GalleryRoute
@Serializable data object AccountRoute

/** The bottom-bar tabs, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
) {
    Dex(DexRoute, R.string.tab_dex, R.drawable.ic_grid_view_filled, R.drawable.ic_grid_view_outlined),
    Detect(DetectRoute, R.string.tab_detect, R.drawable.ic_photo_camera_filled, R.drawable.ic_photo_camera_outlined),
    Gallery(GalleryRoute, R.string.tab_gallery, R.drawable.ic_photo_library_filled, R.drawable.ic_photo_library_outlined),
    Account(AccountRoute, R.string.tab_account, R.drawable.ic_account_circle_filled, R.drawable.ic_account_circle_outlined),
}

/**
 * Switches tabs the standard way: keep one copy of each tab on the back stack, save and
 * restore each tab's own state, and let Back from any tab return to the start tab (Dex).
 */
fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
