package com.rpeters1430.jellyfintv.utils

import com.rpeters1430.jellyfintv.models.BaseItemDto

/**
 * Helper functions for building Jellyfin image URLs.
 */
object ImageUrlBuilder {

    /**
     * Returns the primary image URL for [item].
     *
     * @param serverUrl Base server URL (e.g. "http://192.168.1.100:8096")
     * @param item      The media item.
     * @param maxWidth  Optional max width for image resize.
     */
    fun primaryImage(
        serverUrl: String,
        item: BaseItemDto,
        maxWidth: Int = 400
    ): String? {
        val tag = item.imageTags?.get("Primary") ?: return null
        val base = serverUrl.trimEnd('/')
        return "$base/Items/${item.id}/Images/Primary?tag=$tag&maxWidth=$maxWidth"
    }

    /**
     * Returns the backdrop image URL for [item] (first backdrop), or falls back to primary.
     */
    fun backdropImage(
        serverUrl: String,
        item: BaseItemDto,
        maxWidth: Int = 1280
    ): String? {
        val base = serverUrl.trimEnd('/')
        val backdropTag = item.backdropImageTags?.firstOrNull()
        if (backdropTag != null) {
            return "$base/Items/${item.id}/Images/Backdrop?tag=$backdropTag&maxWidth=$maxWidth"
        }
        return primaryImage(serverUrl, item, maxWidth)
    }

    /**
     * Builds a direct-stream URL for video playback.
     *
     * @param serverUrl Base server URL
     * @param itemId    Media item ID
     * @param userId    Current user ID
     * @param token     Access token
     * @param container Output container (default: "mp4")
     */
    fun videoStreamUrl(
        serverUrl: String,
        itemId: String,
        userId: String,
        token: String,
        container: String = "mp4"
    ): String {
        val base = serverUrl.trimEnd('/')
        return "$base/Videos/$itemId/stream.$container" +
                "?userId=$userId" +
                "&api_key=$token" +
                "&static=true"
    }
}
