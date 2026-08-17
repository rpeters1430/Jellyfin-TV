package com.example.jellyfintv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySort(val label: String, val sortBy: String, val sortOrder: String) {
    TITLE_ASC("A-Z", "SortName", "Ascending"),
    TITLE_DESC("Z-A", "SortName", "Descending"),
    DATE_ADDED("Recently Added", "DateCreated", "Descending"),
    RATING("Top Rated", "CommunityRating", "Descending")
}

class LibraryViewModel(
    val parentId: String? = null,
    val collectionType: String? = null,
    val mediaType: String? = null,
    private val repository: JellyfinRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val items: List<MediaItem> = emptyList(),
        val selectedSort: LibrarySort = LibrarySort.TITLE_ASC,
        val errorMessage: String? = null,
        val userLibraries: List<MediaItem> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isDemoMode: Boolean get() = repository.isDemoMode

    init {
        loadItems()
        loadLibraries()
    }

    // Powers the AppNavHeader tab bar; failures are silently ignored (worst case the tab bar
    // just shows Home/Search with no library tabs) since it's secondary to this screen's content.
    private fun loadLibraries() {
        viewModelScope.launch {
            repository.getUserViews().onSuccess { views ->
                _uiState.update { it.copy(userLibraries = views) }
            }
        }
    }

    fun setSort(sort: LibrarySort) {
        _uiState.update { it.copy(selectedSort = sort) }
        loadItems()
    }

    fun retry() = loadItems()

    fun getPosterUrl(id: String): String = repository.getPosterUrl(id)
    fun getImageHeaders(): Map<String, String> = repository.getStreamHeaders()

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val sort = _uiState.value.selectedSort

            val result = if (!parentId.isNullOrBlank()) {
                repository.getItemsByParent(parentId, sortBy = sort.sortBy, sortOrder = sort.sortOrder)
            } else if (!mediaType.isNullOrBlank()) {
                repository.getLibraryItems(includeItemTypes = mediaType, sortBy = sort.sortBy, sortOrder = sort.sortOrder)
            } else {
                val types = when (collectionType?.lowercase()) {
                    "movies" -> "Movie"
                    "tvshows" -> "Series"
                    "music" -> "MusicAlbum,Audio"
                    "homevideos", "photos" -> "Video,Photo"
                    else -> "Movie,Series,MusicAlbum,Video"
                }
                repository.getLibraryItems(includeItemTypes = types, sortBy = sort.sortBy, sortOrder = sort.sortOrder)
            }

            result.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, items = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load library") }
            }
        }
    }
}
