package com.rpeters1430.jellyfintv.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.repository.JellyfinRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class BrowseViewModel(private val repository: JellyfinRepository) : ViewModel() {

    sealed class BrowseState {
        object Loading : BrowseState()
        data class Ready(
            val libraries: List<BaseItemDto>,
            val continueWatching: List<BaseItemDto>,
            val latestMovies: List<BaseItemDto>,
            val latestShows: List<BaseItemDto>
        ) : BrowseState()
        data class Error(val message: String) : BrowseState()
    }

    private val _state = MutableLiveData<BrowseState>(BrowseState.Loading)
    val state: LiveData<BrowseState> = _state

    private val _searchResults = MutableLiveData<List<BaseItemDto>>(emptyList())
    val searchResults: LiveData<List<BaseItemDto>> = _searchResults

    fun loadContent() {
        _state.value = BrowseState.Loading
        viewModelScope.launch {
            val librariesDeferred = async { repository.getLibraries() }
            val latestMoviesDeferred = async {
                repository.getLatestItems(includeItemTypes = "Movie", limit = 16)
            }
            val latestShowsDeferred = async {
                repository.getLatestItems(includeItemTypes = "Series", limit = 16)
            }
            val continueWatchingDeferred = async {
                repository.getItems(
                    includeItemTypes = "Movie,Episode",
                    limit = 10
                )
            }

            val libraries = librariesDeferred.await().getOrElse { emptyList() }
            val latestMovies = latestMoviesDeferred.await().getOrElse { emptyList() }
            val latestShows = latestShowsDeferred.await().getOrElse { emptyList() }
            // Filter in-progress items on the client side
            val allItems = continueWatchingDeferred.await().getOrElse { null }
            val continueWatching = allItems?.items
                ?.filter { (it.userData?.playbackPositionTicks ?: 0) > 0 }
                ?: emptyList()

            _state.value = BrowseState.Ready(
                libraries = libraries,
                continueWatching = continueWatching,
                latestMovies = latestMovies,
                latestShows = latestShows
            )
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = repository.searchItems(query).getOrElse { emptyList() }
        }
    }
}
