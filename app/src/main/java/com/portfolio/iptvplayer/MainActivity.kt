package com.portfolio.iptvplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.portfolio.iptvplayer.presentation.navigation.AppNavHost
import com.portfolio.iptvplayer.presentation.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity for the whole app. Every screen (channel list,
 * playback, settings) is a composable destination inside AppNavHost's
 * NavHost — replacing the three-Activity, Fragment-based Leanback setup.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppNavHost()
            }
        }
    }
}