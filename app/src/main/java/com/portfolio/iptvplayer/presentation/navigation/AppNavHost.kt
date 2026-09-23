package com.portfolio.iptvplayer.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.portfolio.iptvplayer.presentation.channels.ChannelListScreen
import com.portfolio.iptvplayer.presentation.playback.PlaybackScreen
import com.portfolio.iptvplayer.presentation.settings.SettingsScreen

private object Routes {
    const val CHANNELS = "channels"
    const val PLAYBACK = "playback/{channelId}"
    const val SETTINGS = "settings"

    // Channel ids are the raw stream URL (see M3UParser), so the id must be
    // percent-encoded before it can safely sit inside a nav route path
    // segment — a raw "http://..." id would otherwise break route parsing.
    fun playback(channelId: String) = "playback/${Uri.encode(channelId)}"
}

@Composable
fun AppNavHost() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHANNELS) {
        composable(Routes.CHANNELS) {
            ChannelListScreen(
                onChannelClick = { channel -> navController.navigate(Routes.playback(channel.id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.PLAYBACK,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedId = backStackEntry.arguments?.getString("channelId").orEmpty()
            PlaybackScreen(
                channelId = Uri.decode(encodedId),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onDone = { navController.popBackStack() })
        }
    }
}