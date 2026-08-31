package com.portfolio.iptvplayer.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "iptv_player_prefs"
private const val KEY_PLAYLIST_URL = "custom_playlist_url"

/**
 * A single saved string doesn't need DataStore's async ceremony —
 * SharedPreferences is the right-sized tool here and keeps the settings
 * screen's save/load path trivially simple.
 */
@Singleton
class PlaylistPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedUrl(): String? = prefs.getString(KEY_PLAYLIST_URL, null)

    fun saveUrl(url: String) {
        prefs.edit { putString(KEY_PLAYLIST_URL, url) }
    }

    fun clear() {
        prefs.edit { remove(KEY_PLAYLIST_URL) }
    }
}
