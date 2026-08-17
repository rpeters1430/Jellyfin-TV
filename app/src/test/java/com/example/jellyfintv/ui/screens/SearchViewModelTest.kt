package com.example.jellyfintv.ui.screens

import app.cash.turbine.test
import com.example.jellyfintv.MainDispatcherRule
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: JellyfinRepository

    private fun item(id: String, name: String, type: String) = MediaItem(id = id, name = name, type = type)

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        // MockK's relaxed default for a Result<List<...>> return value isn't actually a valid
        // Result, so any call site that unwraps it (e.g. via onSuccess) throws a
        // ClassCastException unless it's explicitly stubbed - needed here now that
        // SearchViewModel also fetches the library list for the nav header's tab bar.
        coEvery { repository.getUserViews() } returns Result.success(emptyList())
    }

    /** Lets the 300ms debounce inside SearchViewModel.onQueryChange elapse. */
    private fun advancePastDebounce() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `blank query clears results without calling the repository`() = runTest {
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("")
        advancePastDebounce()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSearching)
            assertTrue(state.searchResults.isEmpty())
        }
        coVerify(exactly = 0) { repository.searchMedia(any()) }
    }

    @Test
    fun `query change after debounce populates search results`() = runTest {
        val results = listOf(item("m1", "Batman Begins", "Movie"))
        coEvery { repository.searchMedia("batman") } returns Result.success(results)

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("batman")
        advancePastDebounce()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSearching)
            assertEquals(results, state.searchResults)
        }
    }

    @Test
    fun `search failure surfaces an error message`() = runTest {
        coEvery { repository.searchMedia("batman") } returns Result.failure(Exception("network down"))

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("batman")
        advancePastDebounce()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSearching)
            assertEquals("network down", state.errorMessage)
        }
    }

    @Test
    fun `rapid typing cancels the in-flight debounce so only the final query is searched`() = runTest {
        coEvery { repository.searchMedia(any()) } returns Result.success(emptyList())

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("b")
        viewModel.onQueryChange("ba")
        viewModel.onQueryChange("bat")
        advancePastDebounce()

        coVerify(exactly = 0) { repository.searchMedia("b") }
        coVerify(exactly = 0) { repository.searchMedia("ba") }
        coVerify(exactly = 1) { repository.searchMedia("bat") }
    }

    @Test
    fun `setFilter narrows filteredResults without re-querying the repository`() = runTest {
        val movie = item("m1", "Star Odyssey", "Movie")
        val series = item("s1", "Nebula Chronicles", "Series")
        coEvery { repository.searchMedia("star") } returns Result.success(listOf(movie, series))

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("star")
        advancePastDebounce()

        viewModel.setFilter(SearchFilter.MOVIES)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf(movie), state.filteredResults)
        }

        viewModel.setFilter(SearchFilter.SERIES)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf(series), state.filteredResults)
        }

        coVerify(exactly = 1) { repository.searchMedia(any()) }
    }

    @Test
    fun `getPosterUrl and getImageHeaders delegate to the repository`() {
        every { repository.getPosterUrl("x") } returns "poster-url"
        every { repository.getStreamHeaders() } returns mapOf("Authorization" to "auth-header")
        val viewModel = SearchViewModel(repository)

        assertEquals("poster-url", viewModel.getPosterUrl("x"))
        assertEquals(mapOf("Authorization" to "auth-header"), viewModel.getImageHeaders())
    }
}
