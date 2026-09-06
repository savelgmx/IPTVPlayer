package com.portfolio.iptvplayer.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "iptv_player_prefs"
private const val KEY_PLAYLIST_URL = "custom_playlist_url"
private const val KEY_USES_LOCAL_FILE = "uses_local_file"

/**
 * Two independent, mutually-exclusive sources on top of the bundled demo
 * playlist: a remote URL, or a local file the app has already copied into
 * its own sandboxed storage. The boolean flag decides which one wins on
 * load — see PlaylistRepositoryImpl.loadPlaylist().
 */
@Singleton
class PlaylistPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedUrl(): String? = prefs.getString(KEY_PLAYLIST_URL, null)

    fun saveUrl(url: String) {
        prefs.edit {
            putString(KEY_PLAYLIST_URL, url)
            putBoolean(KEY_USES_LOCAL_FILE, false)
        }
    }

    fun usesLocalFile(): Boolean = prefs.getBoolean(KEY_USES_LOCAL_FILE, false)

    fun markUsesLocalFile() {
        prefs.edit { putBoolean(KEY_USES_LOCAL_FILE, true) }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_PLAYLIST_URL)
            putBoolean(KEY_USES_LOCAL_FILE, false)
        }
    }
}
