package com.portfolio.iptvplayer.presentation.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.portfolio.iptvplayer.domain.model.Channel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Tv
/**
 * A single flat list of every channel plus one pinned "Playlist settings"
 * row at the top — no grid, no row categories. TvLazyColumn (not the plain
 * Compose LazyColumn) is what gives this D-pad focus restoration and
 * scroll-to-focused-item behavior for free, the same benefit Leanback's
 * VerticalGridSupportFragment used to provide.
 */
@Composable
fun ChannelListScreen(
    onChannelClick: (Channel) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ChannelsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Reloads whenever this destination becomes visible again — covers
    // returning from Settings after saving a new playlist URL. In
    // Navigation Compose, LocalLifecycleOwner inside a NavHost destination
    // is the NavBackStackEntry's own lifecycle, so ON_RESUME fires exactly
    // when navigating back here, mirroring Fragment.onResume() before.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadChannels()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    )
    {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 32.dp)
        ) {
            item {
                SettingsRow(onClick = onSettingsClick)
            }

            when (val s = state) {
                is ChannelsUiState.Loaded -> {
                    items(s.channels, key = { it.id }) { channel ->
                        ChannelRow(channel = channel, onClick = { onChannelClick(channel) })
                    }
                }
                is ChannelsUiState.Error -> {
                    item {
                        Text(
                            "Couldn't load channels: ${s.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                ChannelsUiState.Loading -> Unit // list stays as-is; a real app would show a spinner
            }
        }
    }
}

//fun Box(modifier: Modifier, content: Any) {}

@Composable
private fun SettingsRow(onClick: () -> Unit) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = { Text("Playlist settings") },
        supportingContent = { Text("Change M3U source") },
        leadingContent = {
            Icon(Icons.Default.Settings, contentDescription = null)
        }
    )
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = { Text(channel.name) },
        supportingContent = channel.groupTitle?.let { { Text(it) } },
        leadingContent = {
            if (channel.logoUrl.isNullOrBlank()) {
                Icon(Icons.Default.Tv, contentDescription = null)
            } else {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
        }
    )
}