package com.birddex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.birddex.app.ui.BirdDexApp
import com.birddex.app.ui.EmailLinkViewModel
import com.birddex.app.ui.Gate
import com.birddex.app.ui.SessionGateViewModel
import com.birddex.app.ui.theme.BirdDexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // The same instances BirdDexApp gets from hiltViewModel() (both use this activity's store).
    private val sessionGate: SessionGateViewModel by viewModels()
    private val emailLinks: EmailLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: shows Theme.BirdDex.Starting's splash, then Theme.BirdDex.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keeps the logo up for the moment the saved session takes to read from the phone,
        // instead of a blank screen before sign-in or the tabs.
        splash.setKeepOnScreenCondition { sessionGate.gate.value == Gate.Loading }
        // A restored activity already took its link (or still holds it in EmailLinkViewModel).
        if (savedInstanceState == null) openEmailLink(intent)
        enableEdgeToEdge()
        setContent {
            BirdDexTheme {
                BirdDexApp()
            }
        }
    }

    /** A link tapped while the app is open (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openEmailLink(intent)
    }

    /** App Links (A6.2, AndroidManifest): verify-email and reset-password links from emails. */
    private fun openEmailLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        // Reopened from Recents with the old link: it was opened already.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        emailLinks.open(intent.dataString)
    }
}
