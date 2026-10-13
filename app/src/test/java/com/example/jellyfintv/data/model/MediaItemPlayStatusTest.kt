package com.example.jellyfintv.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemPlayStatusTest {

    @Test
    fun `isPlayed returns true when played is true`() {
        val item = MediaItem(
            id = "test-1",
            name = "Movie 1",
            userData = UserData(played = true)
        )
        assertTrue(item.isPlayed)
    }

    @Test
    fun `isPlayed returns true when playedPercentage is 100`() {
        val item = MediaItem(
            id = "test-2",
            name = "Movie 2",
            userData = UserData(played = false, playedPercentage = 100f)
        )
        assertTrue(item.isPlayed)
    }

    @Test
    fun `isPlayed returns false when unplayed`() {
        val item = MediaItem(
            id = "test-3",
            name = "Movie 3",
            userData = UserData(played = false, playedPercentage = 45f)
        )
        assertFalse(item.isPlayed)
    }

    @Test
    fun `isFavorite returns correct status`() {
        val favItem = MediaItem(id = "fav", name = "Favorite Movie", userData = UserData(isFavorite = true))
        val notFavItem = MediaItem(id = "not-fav", name = "Normal Movie", userData = UserData(isFavorite = false))

        assertTrue(favItem.isFavorite)
        assertFalse(notFavItem.isFavorite)
    }

    @Test
    fun `playbackProgress uses playedPercentage when present`() {
        val item = MediaItem(
            id = "test-progress-1",
            name = "In Progress",
            userData = UserData(playedPercentage = 63.5f)
        )
        assertEquals(63.5f, item.playbackProgress, 0.01f)
    }

    @Test
    fun `playbackProgress calculates from ticks when playedPercentage is zero`() {
        val item = MediaItem(
            id = "test-progress-2",
            name = "In Progress from Ticks",
            runTimeTicks = 10_000_000_000L,
            userData = UserData(playbackPositionTicks = 5_000_000_000L, playedPercentage = 0f)
        )
        assertEquals(50.0f, item.playbackProgress, 0.01f)
    }

    @Test
    fun `unplayedCount extracts unplayedItemCount`() {
        val series = MediaItem(
            id = "series-1",
            name = "TV Show",
            userData = UserData(unplayedItemCount = 7)
        )
        assertEquals(7, series.unplayedCount)
    }
}
