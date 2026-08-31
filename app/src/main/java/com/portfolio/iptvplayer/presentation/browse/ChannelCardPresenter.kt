package com.portfolio.iptvplayer.presentation.browse

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

private const val CARD_WIDTH_DP = 200
private const val CARD_HEIGHT_DP = 200

/**
 * Renders each channel as a focusable ImageCardView. Leanback handles the
 * focus scale/elevation animation on D-pad navigation automatically — this
 * class only needs to bind data, not implement any remote-control logic.
 *
 * Glide is used here specifically because this view gets recycled
 * constantly as the D-pad selection moves across the grid; its bitmap pool
 * avoids the per-scroll allocation churn that would otherwise show up as
 * dropped frames on lower-end TV hardware.
 */
class ChannelCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH_DP.dpToPx(context), CARD_HEIGHT_DP.dpToPx(context))
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val channel = (item as GridItem.ChannelItem).channel
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = channel.name
        cardView.contentText = channel.groupTitle.orEmpty()
        cardView.setMainImageDimensions(CARD_WIDTH_DP.dpToPx(cardView.context), CARD_HEIGHT_DP.dpToPx(cardView.context))

        if (channel.logoUrl.isNullOrBlank()) {
            cardView.mainImage = null
        } else {
            Glide.with(cardView.context)
                .asDrawable()
                .load(channel.logoUrl)
                .centerCrop()
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        cardView.mainImage = resource
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        cardView.mainImage = placeholder
                    }
                })
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        Glide.with(cardView.context).clear(cardView)
        cardView.mainImage = null
    }
}

private fun Int.dpToPx(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
