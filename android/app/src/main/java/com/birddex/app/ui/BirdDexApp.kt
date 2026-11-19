package com.birddex.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.birddex.app.ui.account.AccountScreen
import com.birddex.app.ui.detect.DetectScreen
import com.birddex.app.ui.dex.DexScreen
import com.birddex.app.ui.gallery.GalleryScreen
import com.birddex.app.ui.navigation.AccountRoute
import com.birddex.app.ui.navigation.DetectRoute
import com.birddex.app.ui.navigation.DexRoute
import com.birddex.app.ui.navigation.GalleryRoute
import com.birddex.app.ui.navigation.TopLevelDestination
import com.birddex.app.ui.navigation.navigateToTopLevel

/** App shell: bottom navigation bar + one NavHost for all tabs. */
@Composable
fun BirdDexApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination.isOn(destination)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = {
                            Icon(
                                painterResource(
                                    if (selected) destination.selectedIcon else destination.unselectedIcon,
                                ),
                                // The label below already names the tab for screen readers.
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DexRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<DexRoute> { DexScreen() }
            composable<DetectRoute> { DetectScreen() }
            composable<GalleryRoute> { GalleryScreen() }
            composable<AccountRoute> { AccountScreen() }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
