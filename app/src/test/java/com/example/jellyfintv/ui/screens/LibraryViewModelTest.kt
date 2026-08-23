package com.example.jellyfintv.ui.screens

import app.cash.turbine.test
import com.example.jellyfintv.MainDispatcherRule
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: JellyfinRepository

    private fun item(id: String, name: String, type: String = "Movie") = MediaItem(id = id, name = name, type = type)

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        // MockK's relaxed default for a Result<List<...>> return value isn't actually a valid
        // Result, so any call site that unwraps it (e.g. via onSuccess) throws a
        // ClassCastException unless it's explicitly stubbed - needed here now that
        // LibraryViewModel also fetches the library list for the nav header's tab bar.
        coEvery { repository.getUserViews() } returns Result.success(emptyList())
    }

    @Test
    fun `loads items by parentId when provided`() = runTest {
        val musicAlbum = item("music-1", "Cosmic Ambience", type = "MusicAlbum")
        coEvery { repository.getItemsByParent("music-parent-id", any(), any(), any()) } returns Result.success(listOf(musicAlbum))

        val viewModel = LibraryViewModel(parentId = "music-parent-id", collectionType = "music", repository = repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(listOf(musicAlbum), state.items)
        }
    }

    @Test
    fun `loads items by mediaType when parentId is not provided`() = runTest {
        val movieItem = item("m1", "Star Odyssey", type = "Movie")
        coEvery { repository.getLibraryItems(includeItemTypes = "Movie", any(), any()) } returns Result.success(listOf(movieItem))

        val viewModel = LibraryViewModel(mediaType = "Movie", repository = repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(listOf(movieItem), state.items)
        }
    }

    @Test
    fun `loads playlist items when collectionType is playlists`() = runTest {
        val playlistItem = item("pl-1", "Lofi Beats", type = "Playlist")
        coEvery { repository.getLibraryItems(includeItemTypes = "Playlist", any(), any()) } returns Result.success(listOf(playlistItem))

        val viewModel = LibraryViewModel(collectionType = "playlists", repository = repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(listOf(playlistItem), state.items)
        }
    }
}
