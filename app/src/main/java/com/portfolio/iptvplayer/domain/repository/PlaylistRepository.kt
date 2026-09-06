package com.portfolio.iptvplayer.domain.repository

import com.portfolio.iptvplayer.domain.model.Channel

/**
 * Lives in the domain package and references only domain models — no
 * Context, no SharedPreferences, no Retrofit-equivalent types. The data
 * layer (PlaylistRepositoryImpl) implements this; presentation depends on
 * use cases, which depend on this interface, never on the impl directly.
 */
interface PlaylistRepository {
    /** Loads from whichever source is currently active — a previously
     * imported local file, a saved remote URL, or (if neither is set) the
     * bundled demo playlist. Falls back to the bundled playlist if the
     * active source fails to load, so the app never shows a blank screen
     * just because a personal server or file went missing. */
    suspend fun loadPlaylist(): Result<List<Channel>>

    suspend fun loadDefaultPlaylist(): Result<List<Channel>>

    suspend fun loadPlaylistFromUrl(url: String): Result<List<Channel>>

    /** Reads the file at [uriString] (a content:// or file:// URI from a
     * document picker), copies its contents into the app's own storage so
     * future loads don't depend on that URI's grant still being valid, and
     * makes it the active source. */
    suspend fun importPlaylistFromLocalFile(uriString: String): Result<List<Channel>>

    /** Reads a fixed filename from the app's external files directory —
     * the guaranteed-to-work path for TV boxes with no document picker
     * app installed: `adb push yourlist.m3u` into that folder, then call
     * this. See README for the exact adb command. */
    suspend fun importPlaylistFromAppFolder(): Result<List<Channel>>

    fun hasCustomPlaylist(): Boolean
    fun getSavedPlaylistUrl(): String?
    fun saveCustomPlaylistUrl(url: String)
    fun clearCustomPlaylistUrl()
}
