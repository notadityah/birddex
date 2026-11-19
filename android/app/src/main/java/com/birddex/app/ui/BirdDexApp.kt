package com.birddex.app.ui

import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.birddex.app.ui.auth.ForgotPasswordScreen
import com.birddex.app.ui.auth.LoginScreen
import com.birddex.app.ui.auth.RegisterScreen
import com.birddex.app.ui.auth.VerifyEmailScreen
import com.birddex.app.ui.detect.DetectScreen
import com.birddex.app.ui.dex.DexScreen
import com.birddex.app.ui.gallery.GalleryScreen
import com.birddex.app.ui.navigation.AccountRoute
import com.birddex.app.ui.navigation.DetectRoute
import com.birddex.app.ui.navigation.DexRoute
import com.birddex.app.ui.navigation.ForgotPasswordRoute
import com.birddex.app.ui.navigation.GalleryRoute
import com.birddex.app.ui.navigation.LoginRoute
import com.birddex.app.ui.navigation.RegisterRoute
import com.birddex.app.ui.navigation.TopLevelDestination
import com.birddex.app.ui.navigation.VerifyEmailRoute
import com.birddex.app.ui.navigation.isAuthScreen
import com.birddex.app.ui.navigation.leaveAuthFlow
import com.birddex.app.ui.navigation.navigateToTopLevel
import com.birddex.app.ui.navigation.openLogin
import com.birddex.app.ui.navigation.openRegister

/** App shell: bottom navigation bar + one NavHost for the tabs and the sign-in screens. */
@Composable
fun BirdDexApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (!currentDestination.isAuthScreen()) {
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DexRoute,
            // Consumed so the auth screens' own top bar doesn't add the status bar inset twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<DexRoute> { DexScreen() }
            composable<DetectRoute> { DetectScreen() }
            composable<GalleryRoute> { GalleryScreen() }
            composable<AccountRoute> {
                AccountScreen(
                    onSignIn = { navController.openLogin() },
                    onCreateAccount = { navController.openRegister() },
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onSignedIn = { navController.leaveAuthFlow() },
                    onNeedsVerification = { email -> navController.navigate(VerifyEmailRoute(email)) },
                    onCreateAccount = { navController.openRegister() },
                    onForgotPassword = { email -> navController.navigate(ForgotPasswordRoute(email.ifBlank { null })) },
                )
            }
            composable<ForgotPasswordRoute> {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onSignIn = { email -> navController.openLogin(email.ifBlank { null }) },
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = { email ->
                        navController.navigate(VerifyEmailRoute(email)) {
                            // Back from "check your inbox" shouldn't return to a filled-in form.
                            popUpTo<RegisterRoute> { inclusive = true }
                        }
                    },
                    onSignedIn = { navController.leaveAuthFlow() },
                    onSignIn = { navController.openLogin() },
                )
            }
            composable<VerifyEmailRoute> {
                VerifyEmailScreen(
                    onBack = { navController.popBackStack() },
                    onSignIn = { email -> navController.openLogin(email) },
                )
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
