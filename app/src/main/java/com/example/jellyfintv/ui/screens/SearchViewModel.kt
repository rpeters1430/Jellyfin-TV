package com.example.jellyfintv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("TV Shows"),
    PLAYLISTS("Playlists")
}

class SearchViewModel(
    private val repository: JellyfinRepository
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val selectedFilter: SearchFilter = SearchFilter.ALL,
        val isSearching: Boolean = false,
        val searchResults: List<MediaItem> = emptyList(),
        val errorMessage: String? = null,
        val userLibraries: List<MediaItem> = emptyList()
    ) {
        val filteredResults: List<MediaItem>
            get() = when (selectedFilter) {
                SearchFilter.ALL -> searchResults
                SearchFilter.MOVIES -> searchResults.filter { it.type.equals("Movie", ignoreCase = true) }
                SearchFilter.SERIES -> searchResults.filter {
                    it.type.equals("Series", ignoreCase = true) || it.type.equals("Episode", ignoreCase = true)
                }
                SearchFilter.PLAYLISTS -> searchResults.filter {
                    it.type.equals("Playlist", ignoreCase = true) || it.collectionType?.equals("playlists", ignoreCase = true) == true
                }
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isDemoMode: Boolean get() = repository.isDemoMode

    private var searchJob: Job? = null

    init {
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

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList(), errorMessage = null) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            repository.searchMedia(newQuery)
                .onSuccess { items ->
                    _uiState.update { it.copy(isSearching = false, searchResults = items) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSearching = false, errorMessage = e.message ?: "Search failed") }
                }
        }
    }

    fun setFilter(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun getPosterUrl(id: String): String = repository.getPosterUrl(id)
    fun getImageHeaders(): Map<String, String> = repository.getStreamHeaders()
}
