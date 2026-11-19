package com.birddex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.birddex.app.ui.theme.BirdDexTheme
import com.birddex.app.ui.theme.ThemeShowcase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirdDexTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Replaced by the navigation skeleton in A1.4.
                    ThemeShowcase(Modifier.padding(innerPadding))
                }
            }
        }
    }
}
