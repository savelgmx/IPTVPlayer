  package com.portfolio.iptvplayer.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.portfolio.iptvplayer.domain.usecase.ClearCustomPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.GetSavedPlaylistUrlUseCase
import com.portfolio.iptvplayer.domain.usecase.ImportPlaylistFromAppFolderUseCase
import com.portfolio.iptvplayer.domain.usecase.ImportPlaylistFromLocalFileUseCase
import com.portfolio.iptvplayer.domain.usecase.SaveCustomPlaylistUrlUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

private const val ACTION_ID_URL = 1L
private const val ACTION_ID_SAVE_URL = 2L
private const val ACTION_ID_PICK_FILE = 3L
private const val ACTION_ID_LOAD_FROM_APP_FOLDER = 4L
private const val ACTION_ID_CLEAR = 5L

/**
 * Three independent ways to point the app at a real playlist, because no
 * single one is guaranteed to work on every Android TV box:
 *  - Paste a URL (works everywhere, needs a reachable server)
 *  - Pick a local file via the system document picker (works on the
 *    emulator and boxes with a file manager installed — not guaranteed on
 *    stock TV firmware, which often ships with no document provider)
 *  - Load a fixed filename from the app's own external files directory,
 *    populated via `adb push` — always works, no picker or permission
 *    needed, since apps can always read/write their own external files
 *    directory regardless of scoped storage restrictions.
 */
class PlaylistSettingsFragment : GuidedStepSupportFragment() {

    private val entryPoint: SettingsEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            requireContext().applicationContext,
            SettingsEntryPoint::class.java
        )
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            entryPoint.importFromLocalFile()(uri.toString())
                .onSuccess {
                    Toast.makeText(requireContext(), "Loaded ${it.size} channels", Toast.LENGTH_LONG).show()
                    requireActivity().finish()
                }
                .onFailure {
                    Toast.makeText(requireContext(), "Couldn't read that file: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Playlist source",
            "Paste an M3U URL, pick a local file, or load one pushed into the app's folder via adb. See the README for the exact adb command.",
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
                .description(if (currentUrl.isBlank()) "Not set" else currentUrl)
                .editTitle(currentUrl)
                .editable(true)
                .editInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_SAVE_URL)
                .title("Save URL & reload")
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_PICK_FILE)
                .title("Pick a local .m3u file")
                .description("Opens the system file picker, if one is available on this device")
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_LOAD_FROM_APP_FOLDER)
                .title("Load IPTV.m3u from app folder")
                .description("Always works — see README for the adb push command")
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
            ACTION_ID_SAVE_URL -> {
                val urlAction = findActionById(ACTION_ID_URL)
                val url = urlAction?.editTitle?.toString()?.trim().orEmpty()
                if (url.isNotBlank()) {
                    entryPoint.saveCustomPlaylistUrl()(url)
                }
                requireActivity().finish()
            }
            ACTION_ID_PICK_FILE -> {
                // "*/*" rather than a specific MIME type: content providers are
                // inconsistent about what MIME type (if any) they report for
                // .m3u/.m3u8 files, and a too-strict filter risks the file not
                // showing up in the picker at all.
                filePickerLauncher.launch(arrayOf("*/*"))
            }
            ACTION_ID_LOAD_FROM_APP_FOLDER -> {
                lifecycleScope.launch {
                    entryPoint.importFromAppFolder()()
                        .onSuccess {
                            Toast.makeText(requireContext(), "Loaded ${it.size} channels", Toast.LENGTH_LONG).show()
                            requireActivity().finish()
                        }
                        .onFailure {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        }
                }
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
    fun importFromLocalFile(): ImportPlaylistFromLocalFileUseCase
    fun importFromAppFolder(): ImportPlaylistFromAppFolderUseCase
}
