package com.birddex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.birddex.app.ui.BirdDexApp
import com.birddex.app.ui.Gate
import com.birddex.app.ui.SessionGateViewModel
import com.birddex.app.ui.theme.BirdDexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** The same instance BirdDexApp gets from hiltViewModel() (both use this activity's store). */
    private val sessionGate: SessionGateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: shows Theme.BirdDex.Starting's splash, then Theme.BirdDex.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keeps the logo up for the moment the saved session takes to read from the phone,
        // instead of a blank screen before sign-in or the tabs.
        splash.setKeepOnScreenCondition { sessionGate.gate.value == Gate.Loading }
        enableEdgeToEdge()
        setContent {
            BirdDexTheme {
                BirdDexApp()
            }
        }
    }
}
