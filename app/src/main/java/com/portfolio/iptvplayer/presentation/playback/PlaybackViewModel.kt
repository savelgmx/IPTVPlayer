package com.portfolio.iptvplayer.presentation.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.iptvplayer.domain.model.Channel
import com.portfolio.iptvplayer.domain.usecase.GetChannelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaybackUiState {
    data object Loading : PlaybackUiState()
    data class Ready(val channel: Channel) : PlaybackUiState()
    data class Error(val message: String) : PlaybackUiState()
}

/**
 * Holds the full ordered channel list so Up/Down on the remote can move to
 * the next/previous channel without the Activity ever going back to the
 * grid. Depends only on GetChannelsUseCase, same as ChannelsViewModel —
 * both go through the identical domain entry point, so "the channel list"
 * means exactly one thing across the whole app.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var channels: List<Channel> = emptyList()
    private var currentIndex = 0
    private var started = false

    fun start(initialChannelId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            getChannels()
                .onSuccess { list ->
                    channels = list
                    currentIndex = list.indexOfFirst { it.id == initialChannelId }.coerceAtLeast(0)
                    emitCurrent()
                }
                .onFailure { _state.value = PlaybackUiState.Error(it.message ?: "Failed to load channels") }
        }
    }

    fun next() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex + 1) % channels.size
        emitCurrent()
    }

    fun previous() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex - 1 + channels.size) % channels.size
        emitCurrent()
    }

    private fun emitCurrent() {
        _state.value = PlaybackUiState.Ready(channels[currentIndex])
    }
}
