package com.portfolio.iptvplayer.data.repository

import android.content.Context
import com.portfolio.iptvplayer.data.m3u.M3UParser
import com.portfolio.iptvplayer.domain.model.Channel
import com.portfolio.iptvplayer.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_PLAYLIST_ASSET = "default_playlist.m3u"

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PlaylistPrefs
) : PlaylistRepository {

    override suspend fun loadPlaylist(): Result<List<Channel>> {
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

    override fun hasCustomPlaylist(): Boolean = !prefs.getSavedUrl().isNullOrBlank()

    override fun getSavedPlaylistUrl(): String? = prefs.getSavedUrl()

    override fun saveCustomPlaylistUrl(url: String) = prefs.saveUrl(url)

    override fun clearCustomPlaylistUrl() = prefs.clear()
}
