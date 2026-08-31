package com.portfolio.iptvplayer.presentation.browse

import com.portfolio.iptvplayer.domain.model.Channel

/**
 * The grid shows two kinds of cards: real channels, and a single "Playlist
 * settings" entry pinned first. A sealed class + ClassPresenterSelector is
 * the idiomatic Leanback way to mix card types in one adapter without
 * resorting to view-type integers or unchecked casts scattered through the
 * click listener.
 */
sealed class GridItem {
    data class ChannelItem(val channel: Channel) : GridItem()
    data object PlaylistSettingsItem : GridItem()
}
