package com.portfolio.iptvplayer.presentation.settings

import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.portfolio.iptvplayer.domain.usecase.ClearCustomPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.GetSavedPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.SaveCustomPlaylistUrlUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private const val ACTION_ID_URL = 1L
private const val ACTION_ID_SAVE = 2L
private const val ACTION_ID_CLEAR = 3L

/**
 * Leanback's standard pattern for D-pad-friendly text entry on TV: an
 * editable GuidedAction opens the system's TV-optimized keyboard instead of
 * relying on an EditText inside a Dialog, which is painful to navigate with
 * a remote.
 *
 * Depends only on use cases (never PlaylistPrefs or the repository
 * directly) — same rule as every ViewModel in the app. GuidedStepSupportFragment
 * isn't a Hilt entry point on its own, so EntryPointAccessors is the
 * sanctioned way to reach into the Hilt graph from here.
 */
class PlaylistSettingsFragment : GuidedStepSupportFragment() {

    private val entryPoint: SettingsEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            requireContext().applicationContext,
            SettingsEntryPoint::class.java
        )
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Playlist source",
            "Paste your M3U playlist URL (IPTV subscription, personal server, etc). Leave blank to use the bundled test streams.",
            null,
            null
        )
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val currentUrl = entryPoint.getSavedPlaylistUrl()().orEmpty()

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_URL)
                .title("M3U URL")
                .description(if (currentUrl.isBlank()) "Not set — using bundled test streams" else currentUrl)
                .editTitle(currentUrl)
                .editable(true)
                .editInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_SAVE)
                .title("Save & reload")
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_CLEAR)
                .title("Clear (use bundled test streams)")
                .build()
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_ID_SAVE -> {
                val urlAction = findActionById(ACTION_ID_URL)
                val url = urlAction?.editTitle?.toString()?.trim().orEmpty()
                if (url.isNotBlank()) {
                    entryPoint.saveCustomPlaylistUrl()(url)
                }
                requireActivity().finish()
            }
            ACTION_ID_CLEAR -> {
                entryPoint.clearCustomPlaylistUrl()()
                requireActivity().finish()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun getSavedPlaylistUrl(): GetSavedPlaylistUrlUseCase
    fun saveCustomPlaylistUrl(): SaveCustomPlaylistUrlUseCase
    fun clearCustomPlaylistUrl(): ClearCustomPlaylistUrlUseCase
}
