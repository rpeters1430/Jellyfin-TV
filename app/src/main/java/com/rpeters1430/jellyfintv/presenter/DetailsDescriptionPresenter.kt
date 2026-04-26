package com.rpeters1430.jellyfintv.presenter

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.rpeters1430.jellyfintv.models.BaseItemDto

/**
 * Presents a [BaseItemDto]'s title, subtitle, and overview in the Details screen.
 */
class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(viewHolder: ViewHolder, item: Any) {
        val media = item as BaseItemDto
        viewHolder.title.text = media.name

        // Build subtitle: "Year  •  X min  ★ rating"
        val subtitle = buildString {
            media.productionYear?.let { append(it) }
            media.durationMinutes?.let {
                if (isNotEmpty()) append("  •  ")
                append("$it min")
            }
            media.communityRating?.let {
                if (isNotEmpty()) append("  ★ ")
                append(String.format("%.1f", it))
            }
        }
        viewHolder.subtitle.text = subtitle

        viewHolder.body.text = media.overview ?: ""
    }
}
