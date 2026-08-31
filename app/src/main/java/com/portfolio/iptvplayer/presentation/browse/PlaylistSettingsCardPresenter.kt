package com.portfolio.iptvplayer.presentation.browse

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

private const val CARD_WIDTH_DP = 200
private const val CARD_HEIGHT_DP = 200

/** A plain, logo-less card that always sits first in the grid — the only
 * way to reach playlist settings, deliberately kept out of a separate
 * settings menu so there's nothing extra to discover or navigate through. */
class PlaylistSettingsCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            titleText = "Playlist settings"
            contentText = "Change M3U source"
            setMainImageDimensions(CARD_WIDTH_DP.dpToPx(context), CARD_HEIGHT_DP.dpToPx(context))
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) = Unit

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}

private fun Int.dpToPx(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
