package com.portfolio.iptvplayer.presentation.channels

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

sealed class ChannelsUiState {
    data object Loading : ChannelsUiState()
    data class Loaded(val channels: List<Channel>) : ChannelsUiState()
    data class Error(val message: String) : ChannelsUiState()
}

/**
 * Depends only on GetChannelsUseCase — no repository, no SharedPreferences,
 * no knowledge of where the playlist actually comes from. Loads immediately
 * on creation, which is what makes the app "zero-configuration": there is
 * no setup wizard between install and the first channel being playable.
 */
@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ChannelsUiState>(ChannelsUiState.Loading)
    val state: StateFlow<ChannelsUiState> = _state.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _state.value = ChannelsUiState.Loading
            getChannels()
                .onSuccess { _state.value = ChannelsUiState.Loaded(it) }
                .onFailure { _state.value = ChannelsUiState.Error(it.message ?: "Failed to load playlist") }
        }
    }
}