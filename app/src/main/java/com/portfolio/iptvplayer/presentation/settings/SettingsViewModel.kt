package com.portfolio.iptvplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.iptvplayer.domain.usecase.ClearCustomPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.GetSavedPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.ImportPlaylistFromAppFolderUseCase
import com.portfolio.iptvplayer.domain.usecase.ImportPlaylistFromLocalFileUseCase
import com.portfolio.iptvplayer.domain.usecase.SaveCustomPlaylistUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val urlInput: String = "",
    val message: String? = null,
    /** Set to true once an action completes successfully — the screen
     * observes this to navigate back, replacing the old
     * `requireActivity().finish()` calls from the Fragment version. */
    val done: Boolean = false
)

/**
 * A plain Hilt ViewModel — no EntryPointAccessors hack needed anymore.
 * That workaround existed only because GuidedStepSupportFragment wasn't
 * part of the Hilt/Fragment graph the same way an @AndroidEntryPoint
 * Fragment is; a Composable destination inside a single Activity's NavHost
 * has no such limitation, so this is a straightforward improvement, not
 * just a framework swap.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSavedPlaylistUrl: GetSavedPlaylistUrlUseCase,
    private val saveCustomPlaylistUrl: SaveCustomPlaylistUrlUseCase,
    private val clearCustomPlaylistUrl: ClearCustomPlaylistUrlUseCase,
    private val importFromLocalFile: ImportPlaylistFromLocalFileUseCase,
    private val importFromAppFolder: ImportPlaylistFromAppFolderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(urlInput = getSavedPlaylistUrl().orEmpty()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun onUrlChange(value: String) {
        _state.update { it.copy(urlInput = value) }
    }

    fun saveUrl() {
        val url = _state.value.urlInput.trim()
        if (url.isBlank()) return
        saveCustomPlaylistUrl(url)
        _state.update { it.copy(done = true) }
    }

    fun clearPlaylist() {
        clearCustomPlaylistUrl()
        _state.update { it.copy(done = true) }
    }

    fun importFile(uriString: String) {
        viewModelScope.launch {
            importFromLocalFile(uriString)
                .onSuccess { channels ->
                    _state.update { it.copy(message = "Loaded ${channels.size} channels", done = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = "Couldn't read that file: ${error.message}") }
                }
        }
    }

    fun importAppFolder() {
        viewModelScope.launch {
            importFromAppFolder()
                .onSuccess { channels ->
                    _state.update { it.copy(message = "Loaded ${channels.size} channels", done = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.message) }
                }
        }
    }
}