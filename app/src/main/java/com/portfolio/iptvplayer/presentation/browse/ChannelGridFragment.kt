package com.portfolio.iptvplayer.presentation.browse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.portfolio.iptvplayer.presentation.playback.PlaybackActivity
import com.portfolio.iptvplayer.presentation.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val NUM_COLUMNS = 5

/**
 * A single grid of every channel plus one pinned "Playlist settings" card —
 * no row categories, no side menu, no separate settings screen to hunt for.
 * This is the entire "browse" experience: the app opens directly into
 * something watchable, which is the point of "zero-configuration setup for
 * immediate viewing," while still giving personal use an obvious way to
 * point the app at a real playlist.
 */
@AndroidEntryPoint
class ChannelGridFragment : VerticalGridSupportFragment() {

    private val viewModel: ChannelsViewModel by activityViewModels()
    private val gridAdapter = ArrayObjectAdapter(
        ClassPresenterSelector().apply {
            addClassPresenter(GridItem.ChannelItem::class.java, ChannelCardPresenter())
            addClassPresenter(GridItem.PlaylistSettingsItem::class.java, PlaylistSettingsCardPresenter())
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Leanback's VerticalGridSupportFragment calls its own onCreateView()
        // — which needs gridPresenter/adapter already set — before this
        // fragment's onViewCreated() ever runs. Setting them here in
        // onCreate() (rather than onViewCreated, which is too late) is what
        // avoids a NullPointerException on VerticalGridPresenter.onCreateViewHolder.
        gridPresenter = VerticalGridPresenter().apply {
            numberOfColumns = NUM_COLUMNS
        }
        adapter = gridAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = "Channels"

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is GridItem.ChannelItem -> launchPlayback(item)
                GridItem.PlaylistSettingsItem -> startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
        }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Covers returning from Settings after saving a new playlist URL —
        // simplest possible refresh trigger for an MVP, no event bus needed.
        viewModel.loadChannels()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ChannelsUiState.Loading -> Unit // grid stays as-is; a real app would show a spinner overlay
                    is ChannelsUiState.Loaded -> {
                        gridAdapter.clear()
                        gridAdapter.add(GridItem.PlaylistSettingsItem)
                        gridAdapter.addAll(1, state.channels.map { GridItem.ChannelItem(it) })
                    }
                    is ChannelsUiState.Error -> {
                        Toast.makeText(requireContext(), "Couldn't load channels: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun launchPlayback(item: GridItem.ChannelItem) {
        val intent = Intent(requireContext(), PlaybackActivity::class.java).apply {
            putExtra(PlaybackActivity.EXTRA_CHANNEL_ID, item.channel.id)
        }
        startActivity(intent)
    }
}
