package com.portfolio.iptvplayer.presentation.playback

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.portfolio.iptvplayer.domain.model.Channel
import kotlinx.coroutines.delay
import android.view.KeyEvent as AndroidKeyEvent

private const val CHANNEL_NAME_OVERLAY_MS = 2_500L
private const val CONNECT_TIMEOUT_MS = 8_000
private const val READ_TIMEOUT_MS = 8_000

/**
 * Full-screen ExoPlayer playback with in-player channel switching. The
 * buffer/tuning choices and the HLS/TS mime-hint fallback are unchanged
 * from the Leanback version — none of that logic depended on the UI
 * framework, only the plumbing to reach it (AndroidView + onPreviewKeyEvent
 * instead of a PlayerView-hosting Activity + dispatchKeyEvent) changed.
 */
@Composable
fun PlaybackScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    LaunchedEffect(channelId) { viewModel.start(channelId) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    var currentUrl by remember { mutableStateOf<String?>(null) }
    var retriedAsHls by remember { mutableStateOf(false) }
    var overlayText by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("IPTVPlayer/1.0 (Android TV)")

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setTunnelingEnabled(true))
        }
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(20_000, 60_000, 2_500, 5_000)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
    }

    fun playChannel(channel: Channel, forceHls: Boolean = false) {
        currentUrl = channel.streamUrl
        retriedAsHls = forceHls

        val looksLikeHls = channel.streamUrl.contains(".m3u8", ignoreCase = true)
        val mediaItem = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .apply { if (looksLikeHls || forceHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        if (channel.name.isNotBlank()) overlayText = channel.name
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val url = currentUrl ?: return
                if (!retriedAsHls && !url.contains(".m3u8", ignoreCase = true)) {
                    playChannel(Channel(id = url, name = "", logoUrl = null, groupTitle = null, streamUrl = url), forceHls = true)
                } else {
                    overlayText = "Stream unavailable"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(state) {
        val ready = state as? PlaybackUiState.Ready ?: return@LaunchedEffect
        playChannel(ready.channel)
    }

    LaunchedEffect(overlayText) {
        if (overlayText != null) {
            delay(CHANNEL_NAME_OVERLAY_MS)
            overlayText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                        viewModel.next(); true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN, AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        viewModel.previous(); true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        overlayText?.let { name ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .background(Color(0x99000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(name, color = Color.White)
            }
        }
    }
}