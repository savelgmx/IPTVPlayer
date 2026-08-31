package com.portfolio.iptvplayer.presentation.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
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
import com.portfolio.iptvplayer.R
import com.portfolio.iptvplayer.domain.model.Channel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

private const val CHANNEL_NAME_OVERLAY_MS = 2_500L
private const val CONNECT_TIMEOUT_MS = 8_000
private const val READ_TIMEOUT_MS = 8_000

/**
 * Full-screen ExoPlayer playback with in-player channel switching (D-pad
 * Up/Down or the remote's dedicated Channel+/Channel- keys, if it has them)
 * — surfing channels never needs a trip back to the grid.
 *
 * The buffer/tuning choices here are the actual mechanism behind "no
 * buffering, smooth playback on real HD IPTV streams," not just the test
 * streams: see the comments on loadControl, trackSelector, and the retry
 * logic in playChannel().
 */
@AndroidEntryPoint
class PlaybackActivity : ComponentActivity(R.layout.activity_playback) {

    private val viewModel: PlaybackViewModel by viewModels()
    private var player: ExoPlayer? = null
    private var currentChannelUrl: String? = null
    private var retriedAsHls = false
    private val overlayHandler = Handler(Looper.getMainLooper())

    private lateinit var playerView: PlayerView
    private lateinit var channelNameOverlay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
        if (channelId.isNullOrBlank()) {
            finish()
            return
        }

        playerView = findViewById(R.id.player_view)
        channelNameOverlay = findViewById(R.id.channel_name_overlay)
        playerView.requestFocus()

        initPlayer()
        viewModel.start(channelId)
        observeState()
    }

    private fun initPlayer() {
        // Bigger buffers than a typical on-demand video player: a couple of
        // extra seconds before first frame in exchange for the buffer never
        // running dry on a live stream — the trade-off that actually matters
        // for "channel" playback, where nobody is scrubbing a timeline.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 20_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .build()

        // Real IPTV panels are inconsistent about content-type headers and
        // are frequently slow to respond; generous timeouts and explicit
        // cross-protocol-redirect support avoid spurious failures that a
        // stricter default configuration would surface as "it just doesn't play."
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("IPTVPlayer/1.0 (Android TV)")

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        // Tunneling hands audio/video sync off to the hardware pipeline where
        // supported, which is where a lot of the visible judder on live TS
        // streams actually comes from — ExoPlayer silently ignores this if
        // the device/decoder combination doesn't support it, so it's safe
        // to always request.
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setTunnelingEnabled(true))
        }

        // Some IPTV encoders use decoder profiles a device's primary hardware
        // decoder doesn't advertise support for; falling back to a secondary
        // decoder instead of hard-failing is what keeps those channels
        // playable instead of erroring out immediately.
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        handlePlaybackError()
                    }
                })
            }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is PlaybackUiState.Ready -> playChannel(state.channel)
                    is PlaybackUiState.Error -> {
                        channelNameOverlay.text = "Couldn't load channels: ${state.message}"
                        channelNameOverlay.visibility = android.view.View.VISIBLE
                    }
                    PlaybackUiState.Loading -> Unit
                }
            }
        }
    }

    private fun playChannel(channel: Channel, forceHls: Boolean = false) {
        val exoPlayer = player ?: return
        currentChannelUrl = channel.streamUrl
        retriedAsHls = forceHls

        // Many real-world IPTV playlists point at extensionless URLs that
        // are actually HLS master playlists (the provider serves them from
        // a path with no .m3u8 suffix). ExoPlayer's default content-type
        // sniffing handles raw MPEG-TS fine by reading the stream's magic
        // bytes, but it can't "sniff" a text HLS manifest the same way — so
        // an explicit MIME hint is used whenever the URL suggests HLS, and
        // as a fallback retry on the very first playback error otherwise.
        val looksLikeHls = channel.streamUrl.contains(".m3u8", ignoreCase = true)
        val mediaItem = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .apply {
                if (looksLikeHls || forceHls) setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        showChannelNameOverlay(channel.name)
    }

    private fun handlePlaybackError() {
        val url = currentChannelUrl ?: return
        if (!retriedAsHls && !url.contains(".m3u8", ignoreCase = true)) {
            // First failure on a URL we didn't already tag as HLS: retry once,
            // forcing an HLS mime type, before giving up on the channel.
            val channel = Channel(id = url, name = "", logoUrl = null, groupTitle = null, streamUrl = url)
            playChannel(channel, forceHls = true)
        } else {
            channelNameOverlay.text = "Stream unavailable"
            channelNameOverlay.visibility = android.view.View.VISIBLE
        }
    }

    private fun showChannelNameOverlay(name: String) {
        overlayHandler.removeCallbacksAndMessages(null)
        channelNameOverlay.text = name
        channelNameOverlay.visibility = android.view.View.VISIBLE
        overlayHandler.postDelayed(
            { channelNameOverlay.visibility = android.view.View.GONE },
            CHANNEL_NAME_OVERLAY_MS
        )
    }

    /** Handling this at dispatch time (rather than onKeyDown) guarantees
     * channel switching works regardless of whether PlayerView's own
     * transport controls currently have focus. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    viewModel.next()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    viewModel.previous()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    finish()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        overlayHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }
}
