package com.example.jellyfintv.ui.screens

import app.cash.turbine.test
import com.example.jellyfintv.MainDispatcherRule
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.data.repository.UnauthorizedException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: JellyfinRepository

    private fun movie(id: String) = MediaItem(id = id, name = "Movie $id")

    @org.junit.Before
    fun setup() {
        repository = mockk(relaxed = true)
        coEvery { repository.getUserViews() } returns Result.success(emptyList())
        coEvery { repository.getLatestItemsByParent(any()) } returns Result.success(emptyList())
        coEvery { repository.getItemsByParent(any(), any(), any(), any()) } returns Result.success(emptyList())
    }

    @Test
    fun `successful load populates all rows and focuses the first resume item`() = runTest {
        val resume = listOf(movie("r1"))
        val nextUp = listOf(movie("n1"))
        val movies = listOf(movie("m1"))
        val series = listOf(movie("s1"))
        coEvery { repository.getResumeItems() } returns Result.success(resume)
        coEvery { repository.getNextUpItems() } returns Result.success(nextUp)
        coEvery { repository.getLatestMovies() } returns Result.success(movies)
        coEvery { repository.getLatestSeries() } returns Result.success(series)

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(resume, state.resumeItems)
            assertEquals(nextUp, state.nextUpItems)
            assertEquals(movies, state.latestMovies)
            assertEquals(series, state.latestSeries)
            assertEquals(movie("r1"), state.focusedItem)
            assertNull(state.errorMessage)
            assertFalse(state.sessionExpired)
        }
    }

    @Test
    fun `focuses first latest movie when there is nothing to resume`() = runTest {
        coEvery { repository.getResumeItems() } returns Result.success(emptyList())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(listOf(movie("m1")))
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            assertEquals(movie("m1"), awaitItem().focusedItem)
        }
    }

    @Test
    fun `a 401 on any row sets sessionExpired without an error message`() = runTest {
        coEvery { repository.getResumeItems() } returns Result.failure(UnauthorizedException())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(emptyList())
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.sessionExpired)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `a non-auth failure on one row surfaces an error but keeps the rows that succeeded`() = runTest {
        coEvery { repository.getResumeItems() } returns Result.failure(Exception("boom"))
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(listOf(movie("m1")))
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("boom", state.errorMessage)
            assertEquals(listOf(movie("m1")), state.latestMovies)
            assertFalse(state.sessionExpired)
            assertTrue(state.hasAnyContent)
        }
    }

    @Test
    fun `retry re-fetches all rows`() = runTest {
        coEvery { repository.getResumeItems() } returns Result.success(emptyList())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(emptyList())
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())

        val viewModel = HomeViewModel(repository)
        viewModel.retry()

        coEvery { repository.getLatestMovies() } returns Result.failure(Exception("network down"))
        viewModel.retry()

        viewModel.uiState.test {
            assertEquals("network down", awaitItem().errorMessage)
        }
    }

    @Test
    fun `onItemFocused updates the focused item`() = runTest {
        coEvery { repository.getResumeItems() } returns Result.success(emptyList())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(emptyList())
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())
        val viewModel = HomeViewModel(repository)

        viewModel.onItemFocused(movie("focused-1"))

        viewModel.uiState.test {
            assertEquals(movie("focused-1"), awaitItem().focusedItem)
        }
    }

    @Test
    fun `isDemoMode and url helpers delegate to the repository`() = runTest {
        every { repository.isDemoMode } returns true
        every { repository.getPosterUrl("x") } returns "poster-url"
        every { repository.getBackdropUrl("x") } returns "backdrop-url"
        coEvery { repository.getResumeItems() } returns Result.success(emptyList())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(emptyList())
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())
        val viewModel = HomeViewModel(repository)

        assertTrue(viewModel.isDemoMode)
        assertEquals("poster-url", viewModel.getPosterUrl("x"))
        assertEquals("backdrop-url", viewModel.getBackdropUrl("x"))
    }

    @Test
    fun `custom library like Stuff is populated in librarySections`() = runTest {
        val stuffView = MediaItem(id = "view-stuff", name = "Stuff", type = "CollectionFolder", collectionType = "homevideos")
        val stuffItem = movie("stuff-1")
        coEvery { repository.getResumeItems() } returns Result.success(emptyList())
        coEvery { repository.getNextUpItems() } returns Result.success(emptyList())
        coEvery { repository.getLatestMovies() } returns Result.success(emptyList())
        coEvery { repository.getLatestSeries() } returns Result.success(emptyList())
        coEvery { repository.getUserViews() } returns Result.success(listOf(stuffView))
        coEvery { repository.getLatestItemsByParent("view-stuff") } returns Result.success(listOf(stuffItem))

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.librarySections.size)
            assertEquals("Stuff", state.librarySections.first().title)
            assertEquals(listOf(stuffItem), state.librarySections.first().items)
            assertEquals(stuffItem, state.focusedItem)
        }
    }
}
