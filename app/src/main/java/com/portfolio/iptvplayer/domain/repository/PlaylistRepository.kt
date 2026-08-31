package com.portfolio.iptvplayer.domain.repository

import com.portfolio.iptvplayer.domain.model.Channel

/**
 * Lives in the domain package and references only domain models — no
 * Context, no SharedPreferences, no Retrofit-equivalent types. The data
 * layer (PlaylistRepositoryImpl) implements this; presentation depends on
 * use cases, which depend on this interface, never on the impl directly.
 */
interface PlaylistRepository {
    /** Loads the user's saved M3U URL if one exists; falls back to the
     * bundled demo playlist if there's no saved URL, or if the saved URL
     * fails to load — the app should never show a blank screen just
     * because a personal server hiccuped. */
    suspend fun loadPlaylist(): Result<List<Channel>>

    suspend fun loadDefaultPlaylist(): Result<List<Channel>>

    suspend fun loadPlaylistFromUrl(url: String): Result<List<Channel>>

    fun hasCustomPlaylist(): Boolean
    fun getSavedPlaylistUrl(): String?
    fun saveCustomPlaylistUrl(url: String)
    fun clearCustomPlaylistUrl()
}
