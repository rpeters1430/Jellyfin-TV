package com.example.jellyfintv.ui.screens

import app.cash.turbine.test
import com.example.jellyfintv.MainDispatcherRule
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.data.repository.UnauthorizedException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MediaDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: JellyfinRepository = mockk()

    @Before
    fun setUp() {
        coEvery { repository.getSimilarItems(any()) } returns Result.success(emptyList())
        coEvery { repository.getSeasons(any()) } returns Result.success(emptyList())
        coEvery { repository.getEpisodes(any(), any()) } returns Result.success(emptyList())
        coEvery { repository.getPlaylistItems(any()) } returns Result.success(emptyList())
        coEvery { repository.toggleFavorite(any(), any()) } returns Result.success(true)
        coEvery { repository.togglePlayed(any(), any()) } returns Result.success(com.example.jellyfintv.data.model.UserData(played = true))
    }

    @Test
    fun `requests details for the given itemId and exposes the result`() = runTest {
        val item = MediaItem(id = "item-42", name = "The Movie", userData = com.example.jellyfintv.data.model.UserData(played = false))
        val similar = MediaItem(id = "item-43", name = "Similar Movie")
        coEvery { repository.getItemDetails("item-42") } returns Result.success(item)
        coEvery { repository.getSimilarItems("item-42") } returns Result.success(listOf(similar))

        val viewModel = MediaDetailViewModel("item-42", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(item, state.item)
            assertEquals(listOf(similar), state.similarItems)
            assertFalse(state.isPlayed)
            assertNull(state.errorMessage)
        }
        coVerify(exactly = 1) { repository.getItemDetails("item-42") }
        coVerify(exactly = 1) { repository.getSimilarItems("item-42") }
    }

    @Test
    fun `failure surfaces an error message`() = runTest {
        coEvery { repository.getItemDetails(any()) } returns Result.failure(Exception("not found"))

        val viewModel = MediaDetailViewModel("missing", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("not found", state.errorMessage)
            assertNull(state.item)
        }
    }

    @Test
    fun `401 sets sessionExpired instead of an error message`() = runTest {
        coEvery { repository.getItemDetails(any()) } returns Result.failure(UnauthorizedException())

        val viewModel = MediaDetailViewModel("item-1", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.sessionExpired)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `retry re-fetches the same item`() = runTest {
        coEvery { repository.getItemDetails("item-1") } returns Result.failure(Exception("boom"))
        val viewModel = MediaDetailViewModel("item-1", repository)

        coEvery { repository.getItemDetails("item-1") } returns Result.success(MediaItem(id = "item-1", name = "Fixed"))
        viewModel.retry()

        viewModel.uiState.test {
            assertEquals("Fixed", awaitItem().item?.name)
        }
        coVerify(exactly = 2) { repository.getItemDetails("item-1") }
    }

    @Test
    fun `togglePlayed updates state and calls repository`() = runTest {
        val item = MediaItem(id = "item-10", name = "Interstellar", userData = com.example.jellyfintv.data.model.UserData(played = false))
        coEvery { repository.getItemDetails("item-10") } returns Result.success(item)
        coEvery { repository.togglePlayed("item-10", false) } returns Result.success(com.example.jellyfintv.data.model.UserData(played = true))

        val viewModel = MediaDetailViewModel("item-10", repository)
        viewModel.togglePlayed()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isPlayed)
        }
        coVerify(exactly = 1) { repository.togglePlayed("item-10", false) }
    }

    @Test
    fun `playlist item type loads playlist items and picks next unwatched`() = runTest {
        val playlist = MediaItem(id = "pl-1", name = "YouTube Playlist", type = "Playlist")
        val item1 = MediaItem(id = "v-1", name = "Video 1", type = "Video", userData = com.example.jellyfintv.data.model.UserData(played = true))
        val item2 = MediaItem(id = "v-2", name = "Video 2", type = "Video", userData = com.example.jellyfintv.data.model.UserData(played = false))
        
        coEvery { repository.getItemDetails("pl-1") } returns Result.success(playlist)
        coEvery { repository.getPlaylistItems("pl-1") } returns Result.success(listOf(item1, item2))

        val viewModel = MediaDetailViewModel("pl-1", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isLoadingPlaylist)
            assertEquals(playlist, state.item)
            assertEquals(2, state.playlistItems.size)
            assertEquals(item2, state.nextPlaylistItemToPlay)
        }
        coVerify(exactly = 1) { repository.getPlaylistItems("pl-1") }
    }

    @Test
    fun `series item type loads seasons and episodes and picks next unwatched episode`() = runTest {
        val series = MediaItem(id = "series-1", name = "Starlight Chronicles", type = "Series")
        val season1 = MediaItem(id = "season-1", name = "Season 1", type = "Season", seriesId = "series-1")
        val ep1 = MediaItem(id = "ep-1", name = "Pilot", type = "Episode", seasonId = "season-1", userData = com.example.jellyfintv.data.model.UserData(played = true))
        val ep2 = MediaItem(id = "ep-2", name = "Singularity", type = "Episode", seasonId = "season-1", userData = com.example.jellyfintv.data.model.UserData(played = false))

        coEvery { repository.getItemDetails("series-1") } returns Result.success(series)
        coEvery { repository.getSeasons("series-1") } returns Result.success(listOf(season1))
        coEvery { repository.getEpisodes("series-1", "season-1") } returns Result.success(listOf(ep1, ep2))

        val viewModel = MediaDetailViewModel("series-1", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(series, state.item)
            assertEquals(listOf(season1), state.seasons)
            assertEquals("season-1", state.selectedSeasonId)
            assertEquals(listOf(ep1, ep2), state.episodes)
            assertEquals(ep2, state.nextEpisodeToPlay)
        }
        coVerify(exactly = 1) { repository.getSeasons("series-1") }
        coVerify(exactly = 1) { repository.getEpisodes("series-1", "season-1") }
    }

    @Test
    fun `selectSeason updates selectedSeasonId and loads new season episodes`() = runTest {
        val series = MediaItem(id = "series-1", name = "Starlight Chronicles", type = "Series")
        val season1 = MediaItem(id = "season-1", name = "Season 1", type = "Season", seriesId = "series-1")
        val season2 = MediaItem(id = "season-2", name = "Season 2", type = "Season", seriesId = "series-1")
        val epS2_1 = MediaItem(id = "ep-201", name = "Aftermath", type = "Episode", seasonId = "season-2")

        coEvery { repository.getItemDetails("series-1") } returns Result.success(series)
        coEvery { repository.getSeasons("series-1") } returns Result.success(listOf(season1, season2))
        coEvery { repository.getEpisodes("series-1", "season-1") } returns Result.success(emptyList())
        coEvery { repository.getEpisodes("series-1", "season-2") } returns Result.success(listOf(epS2_1))

        val viewModel = MediaDetailViewModel("series-1", repository)
        viewModel.selectSeason("season-2")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("season-2", state.selectedSeasonId)
            assertEquals(listOf(epS2_1), state.episodes)
        }
        coVerify(exactly = 1) { repository.getEpisodes("series-1", "season-2") }
    }

    @Test
    fun `episode item type loads parent series and sibling episodes`() = runTest {
        val episode = MediaItem(id = "ep-1", name = "Pilot", type = "Episode", seriesId = "series-1", seasonId = "season-1")
        val parentSeries = MediaItem(id = "series-1", name = "Starlight Chronicles", type = "Series")
        val siblingEp = MediaItem(id = "ep-2", name = "Episode 2", type = "Episode", seriesId = "series-1", seasonId = "season-1")

        coEvery { repository.getItemDetails("ep-1") } returns Result.success(episode)
        coEvery { repository.getItemDetails("series-1") } returns Result.success(parentSeries)
        coEvery { repository.getEpisodes("series-1", "season-1") } returns Result.success(listOf(episode, siblingEp))

        val viewModel = MediaDetailViewModel("ep-1", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(episode, state.item)
            assertEquals(parentSeries, state.parentSeries)
            assertEquals(listOf(episode, siblingEp), state.siblingEpisodes)
        }
        coVerify(exactly = 1) { repository.getItemDetails("series-1") }
        coVerify(exactly = 1) { repository.getEpisodes("series-1", "season-1") }
    }

    @Test
    fun `toggleEpisodePlayed updates episode played state`() = runTest {
        val series = MediaItem(id = "series-1", name = "Starlight Chronicles", type = "Series")
        val ep1 = MediaItem(id = "ep-1", name = "Pilot", type = "Episode", userData = com.example.jellyfintv.data.model.UserData(played = false))
        
        coEvery { repository.getItemDetails("series-1") } returns Result.success(series)
        coEvery { repository.getSeasons("series-1") } returns Result.success(emptyList())
        coEvery { repository.getEpisodes("series-1", any()) } returns Result.success(listOf(ep1))
        coEvery { repository.togglePlayed("ep-1", false) } returns Result.success(com.example.jellyfintv.data.model.UserData(played = true))

        val viewModel = MediaDetailViewModel("series-1", repository)
        viewModel.toggleEpisodePlayed(ep1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.episodes.first().userData?.played == true)
        }
        coVerify(exactly = 1) { repository.togglePlayed("ep-1", false) }
    }
}
