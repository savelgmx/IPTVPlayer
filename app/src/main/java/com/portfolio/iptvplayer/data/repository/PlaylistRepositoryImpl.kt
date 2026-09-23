package com.portfolio.iptvplayer.data.repository

import android.content.Context
import android.net.Uri
import com.portfolio.iptvplayer.data.m3u.M3UParser
import com.portfolio.iptvplayer.domain.model.Channel
import com.portfolio.iptvplayer.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_PLAYLIST_ASSET = "default_playlist.m3u"
private const val IMPORTED_PLAYLIST_FILENAME = "imported_playlist.m3u"
private const val APP_FOLDER_PLAYLIST_FILENAME = "IPTV.m3u"

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PlaylistPrefs
) : PlaylistRepository {

    override suspend fun loadPlaylist(): Result<List<Channel>> {
        if (prefs.usesLocalFile()) {
            return readImportedFile().recoverCatching { loadDefaultPlaylist().getOrThrow() }
        }
        val savedUrl = prefs.getSavedUrl()
        if (savedUrl.isNullOrBlank()) return loadDefaultPlaylist()

        return loadPlaylistFromUrl(savedUrl).recoverCatching {
            loadDefaultPlaylist().getOrThrow()
        }
    }

    override suspend fun loadDefaultPlaylist(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val content = context.assets.open(DEFAULT_PLAYLIST_ASSET)
                .bufferedReader()
                .use(BufferedReader::readText)
            Result.success(M3UParser.parse(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadPlaylistFromUrl(url: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val content = URL(url).openStream()
                .let { InputStreamReader(it) }
                .buffered()
                .use(BufferedReader::readText)
            Result.success(M3UParser.parse(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importPlaylistFromLocalFile(uriString: String): Result<List<Channel>> =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use(BufferedReader::readText)
                    ?: return@withContext Result.failure(IllegalStateException("Couldn't open the selected file"))

                // Copy into our own storage immediately: a content:// URI's
                // read grant can be revoked or become invalid after this
                // session (device reboot, picker app updated, etc.), but a
                // file we copied into our own sandbox is ours to keep.
                importedPlaylistFile().writeText(content)
                prefs.markUsesLocalFile()

                Result.success(M3UParser.parse(content))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun importPlaylistFromAppFolder(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.getExternalFilesDir(null), APP_FOLDER_PLAYLIST_FILENAME)
            if (!file.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("No file found at ${file.absolutePath} — see README for the adb push command")
                )
            }
            val content = file.readText()
            importedPlaylistFile().writeText(content)
            prefs.markUsesLocalFile()
            Result.success(M3UParser.parse(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readImportedFile(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val file = importedPlaylistFile()
            if (!file.exists()) return@withContext Result.failure(IllegalStateException("Imported playlist file missing"))
            Result.success(M3UParser.parse(file.readText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importedPlaylistFile(): File = File(context.filesDir, IMPORTED_PLAYLIST_FILENAME)

    override fun hasCustomPlaylist(): Boolean = prefs.usesLocalFile() || !prefs.getSavedUrl().isNullOrBlank()

    override fun getSavedPlaylistUrl(): String? = prefs.getSavedUrl()

    override fun saveCustomPlaylistUrl(url: String) = prefs.saveUrl(url)

    override fun clearCustomPlaylistUrl() {
        prefs.clear()
        importedPlaylistFile().delete()
    }
}