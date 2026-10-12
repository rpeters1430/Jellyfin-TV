package com.example.jellyfintv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.data.repository.UnauthorizedException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibrarySection(
    val id: String,
    val title: String,
    val collectionType: String?,
    val items: List<MediaItem>
)

class HomeViewModel(private val repository: JellyfinRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val resumeItems: List<MediaItem> = emptyList(),
        val nextUpItems: List<MediaItem> = emptyList(),
        val latestMovies: List<MediaItem> = emptyList(),
        val latestSeries: List<MediaItem> = emptyList(),
        val librarySections: List<LibrarySection> = emptyList(),
        val userLibraries: List<MediaItem> = emptyList(),
        val focusedItem: MediaItem? = null,
        val errorMessage: String? = null,
        val sessionExpired: Boolean = false
    ) {
        val hasAnyContent: Boolean
            get() = resumeItems.isNotEmpty() || nextUpItems.isNotEmpty() ||
                latestMovies.isNotEmpty() || latestSeries.isNotEmpty() ||
                librarySections.any { it.items.isNotEmpty() }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isDemoMode: Boolean get() = repository.isDemoMode
    private var lastRefreshTimestamp: Long = 0L

    init {
        load(silent = false)
        observeMediaEvents()
    }

    @OptIn(FlowPreview::class)
    private fun observeMediaEvents() {
        viewModelScope.launch {
            repository.mediaEvents
                .debounce(800L)
                .collect {
                    refresh(silent = true)
                }
        }
    }

    fun retry() = load(silent = false)

    fun refresh(silent: Boolean = true) = load(silent = silent)

    fun onScreenResumed() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTimestamp >= 15_000L) {
            refresh(silent = true)
        }
    }

    fun onItemFocused(media: MediaItem) {
        _uiState.update { it.copy(focusedItem = media) }
    }

    fun getPosterUrl(itemId: String): String = repository.getPosterUrl(itemId)
    fun getBackdropUrl(itemId: String): String = repository.getBackdropUrl(itemId)
    fun getImageHeaders(): Map<String, String> = repository.getStreamHeaders()

    private fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionExpired = false) }
            } else {
                _uiState.update { it.copy(errorMessage = null, sessionExpired = false) }
            }

            var unauthorizedCount = 0
            var errorMessage: String? = null
            var resumeItems: List<MediaItem> = emptyList()
            var nextUpItems: List<MediaItem> = emptyList()
            var latestMovies: List<MediaItem> = emptyList()
            var latestSeries: List<MediaItem> = emptyList()
            var userLibraries: List<MediaItem> = emptyList()
            val librarySections = mutableListOf<LibrarySection>()

            fun <T> handle(result: Result<T>, onSuccess: (T) -> Unit) {
                result.onSuccess(onSuccess).onFailure { e ->
                    if (e is UnauthorizedException) {
                        unauthorizedCount++
                    } else {
                        errorMessage = errorMessage ?: e.message
                    }
                }
            }

            handle(repository.getResumeItems()) { resumeItems = it }
            handle(repository.getNextUpItems()) { nextUpItems = it }
            handle(repository.getLatestMovies()) { latestMovies = it }
            handle(repository.getLatestSeries()) { latestSeries = it }

            // Fetch user libraries (e.g. Movies, TV Shows, Music, Stuff, Home Videos & Photos)
            val viewsResult = repository.getUserViews()
            viewsResult.onSuccess { views ->
                userLibraries = views
                val firstMovieViewId = views.firstOrNull { it.collectionType?.lowercase() == "movies" }?.id
                val firstTvViewId = views.firstOrNull { it.collectionType?.lowercase() == "tvshows" }?.id

                for (view in views) {
                    val isPrimaryMovie = (view.id == firstMovieViewId)
                    val isPrimaryTv = (view.id == firstTvViewId)

                    // Secondary movie/TV libraries, or any other library categories (homevideos, photos, music, etc.)
                    // get their own dedicated section on the home screen
                    if (!isPrimaryMovie && !isPrimaryTv) {
                        val itemsResult = repository.getLatestItemsByParent(view.id)
                        val items = itemsResult.getOrNull()?.ifEmpty {
                            repository.getItemsByParent(view.id).getOrDefault(emptyList())
                        } ?: repository.getItemsByParent(view.id).getOrDefault(emptyList())

                        if (items.isNotEmpty()) {
                            librarySections.add(
                                LibrarySection(
                                    id = view.id,
                                    title = view.name,
                                    collectionType = view.collectionType,
                                    items = items
                                )
                            )
                        }
                    }
                }
            }.onFailure { e ->
                if (e is UnauthorizedException) unauthorizedCount++
            }

            val hasContent = resumeItems.isNotEmpty() || nextUpItems.isNotEmpty() ||
                latestMovies.isNotEmpty() || latestSeries.isNotEmpty() ||
                librarySections.isNotEmpty()

            val isSessionExpired = unauthorizedCount >= 3 || (!hasContent && unauthorizedCount > 0)

            if (isSessionExpired) {
                _uiState.update { it.copy(isLoading = false, sessionExpired = true) }
                return@launch
            }

            lastRefreshTimestamp = System.currentTimeMillis()

            _uiState.update { current ->
                val allNewItems = resumeItems + nextUpItems + latestMovies + latestSeries + librarySections.flatMap { it.items }
                val updatedFocus = current.focusedItem?.let { curr ->
                    allNewItems.find { it.id == curr.id } ?: curr
                } ?: (resumeItems.firstOrNull()
                    ?: latestMovies.firstOrNull()
                    ?: latestSeries.firstOrNull()
                    ?: librarySections.firstOrNull()?.items?.firstOrNull())

                current.copy(
                    isLoading = false,
                    resumeItems = resumeItems,
                    nextUpItems = nextUpItems,
                    latestMovies = latestMovies,
                    latestSeries = latestSeries,
                    librarySections = librarySections,
                    userLibraries = userLibraries,
                    focusedItem = updatedFocus,
                    errorMessage = errorMessage
                )
            }
        }
    }
}
