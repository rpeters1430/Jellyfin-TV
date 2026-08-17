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
import org.junit.Rule
import org.junit.Test

class MediaDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: JellyfinRepository = mockk(relaxed = true)

    @Test
    fun `requests details for the given itemId and exposes the result`() = runTest {
        val item = MediaItem(id = "item-42", name = "The Movie")
        coEvery { repository.getItemDetails("item-42") } returns Result.success(item)

        val viewModel = MediaDetailViewModel("item-42", repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(item, state.item)
            assertNull(state.errorMessage)
        }
        coVerify(exactly = 1) { repository.getItemDetails("item-42") }
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
}
