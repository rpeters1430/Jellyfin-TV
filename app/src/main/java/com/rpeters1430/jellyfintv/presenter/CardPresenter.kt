package com.rpeters1430.jellyfintv.presenter

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.rpeters1430.jellyfintv.R
import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.utils.ImageUrlBuilder

/**
 * Leanback [Presenter] that renders a [BaseItemDto] as an [ImageCardView].
 */
class CardPresenter(private val serverUrl: String) : Presenter() {

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(
                ContextCompat.getColor(parent.context, R.color.card_background)
            )
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val mediaItem = item as BaseItemDto
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = mediaItem.name
        cardView.contentText = mediaItem.productionYear?.toString() ?: mediaItem.type

        val res = cardView.resources
        val cardWidthPx = (CARD_WIDTH * res.displayMetrics.density).toInt()
        val cardHeightPx = (CARD_HEIGHT * res.displayMetrics.density).toInt()
        cardView.setMainImageDimensions(cardWidthPx, cardHeightPx)

        val imageUrl = ImageUrlBuilder.primaryImage(serverUrl, mediaItem, maxWidth = cardWidthPx)
        if (imageUrl != null) {
            cardView.mainImageView?.let { imageView ->
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(cardView.context)
                    .load(imageUrl)
                    .centerCrop()
                    .error(R.drawable.default_background)
                    .into(imageView)
            }
        } else {
            cardView.mainImage = ContextCompat.getDrawable(cardView.context, R.drawable.default_background)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    class ViewHolder(view: ImageCardView) : Presenter.ViewHolder(view)
}
