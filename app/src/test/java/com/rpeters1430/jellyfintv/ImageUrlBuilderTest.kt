package com.rpeters1430.jellyfintv

import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.utils.ImageUrlBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlBuilderTest {

    private val serverUrl = "http://192.168.1.100:8096"

    private fun item(
        id: String = "abc123",
        imageTags: Map<String, String>? = null,
        backdropTags: List<String>? = null
    ) = BaseItemDto(
        id = id,
        name = "Test Item",
        type = "Movie",
        overview = null,
        productionYear = null,
        runTimeTicks = null,
        communityRating = null,
        imageTags = imageTags,
        backdropImageTags = backdropTags,
        collectionType = null,
        mediaType = null,
        seriesId = null,
        seriesName = null,
        parentId = null,
        indexNumber = null,
        parentIndexNumber = null,
        videoType = null,
        isFolder = null,
        genres = null,
        studios = null,
        people = null,
        userData = null
    )

    @Test
    fun `primaryImage returns null when no image tags`() {
        val result = ImageUrlBuilder.primaryImage(serverUrl, item())
        assertNull(result)
    }

    @Test
    fun `primaryImage returns correct URL`() {
        val mediaItem = item(imageTags = mapOf("Primary" to "tag1"))
        val result = ImageUrlBuilder.primaryImage(serverUrl, mediaItem, maxWidth = 400)
        assertEquals(
            "http://192.168.1.100:8096/Items/abc123/Images/Primary?tag=tag1&maxWidth=400",
            result
        )
    }

    @Test
    fun `primaryImage trims trailing slash from server URL`() {
        val mediaItem = item(imageTags = mapOf("Primary" to "tagX"))
        val result = ImageUrlBuilder.primaryImage("http://192.168.1.100:8096/", mediaItem, maxWidth = 200)
        assertEquals(
            "http://192.168.1.100:8096/Items/abc123/Images/Primary?tag=tagX&maxWidth=200",
            result
        )
    }

    @Test
    fun `backdropImage returns backdrop URL when available`() {
        val mediaItem = item(backdropTags = listOf("backdropTag"))
        val result = ImageUrlBuilder.backdropImage(serverUrl, mediaItem, maxWidth = 1280)
        assertEquals(
            "http://192.168.1.100:8096/Items/abc123/Images/Backdrop?tag=backdropTag&maxWidth=1280",
            result
        )
    }

    @Test
    fun `backdropImage falls back to primary when no backdrop tags`() {
        val mediaItem = item(imageTags = mapOf("Primary" to "pTag"))
        val result = ImageUrlBuilder.backdropImage(serverUrl, mediaItem, maxWidth = 1280)
        assertEquals(
            "http://192.168.1.100:8096/Items/abc123/Images/Primary?tag=pTag&maxWidth=1280",
            result
        )
    }

    @Test
    fun `videoStreamUrl constructs correct URL`() {
        val result = ImageUrlBuilder.videoStreamUrl(
            serverUrl = serverUrl,
            itemId = "vid1",
            userId = "user1",
            token = "tok",
            container = "mp4"
        )
        assertEquals(
            "http://192.168.1.100:8096/Videos/vid1/stream.mp4?userId=user1&api_key=tok&static=true",
            result
        )
    }

    @Test
    fun `BaseItemDto durationMinutes converts correctly`() {
        // 9_000_000_000 ticks = 900 seconds = 15 minutes
        val mediaItem = item().copy(runTimeTicks = 9_000_000_000L)
        assertEquals(15, mediaItem.durationMinutes)
    }

    @Test
    fun `BaseItemDto durationMinutes is null when runTimeTicks is null`() {
        val mediaItem = item()
        assertNull(mediaItem.durationMinutes)
    }
}
