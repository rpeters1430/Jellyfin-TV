package com.example.jellyfintv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.data.repository.UnauthorizedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaDetailViewModel(
    private val itemId: String,
    private val repository: JellyfinRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val item: MediaItem? = null,
        val seasons: List<MediaItem> = emptyList(),
        val selectedSeasonId: String? = null,
        val episodes: List<MediaItem> = emptyList(),
        val isLoadingEpisodes: Boolean = false,
        val nextEpisodeToPlay: MediaItem? = null,
        val isFavorite: Boolean = false,
        val errorMessage: String? = null,
        val sessionExpired: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun getPosterUrl(id: String): String = repository.getPosterUrl(id)
    fun getBackdropUrl(id: String): String = repository.getBackdropUrl(id)
    fun getImageHeaders(): Map<String, String> = repository.getStreamHeaders()

    fun toggleFavorite() {
        val currentItem = _uiState.value.item ?: return
        val currentFav = _uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = !currentFav) }
        viewModelScope.launch {
            repository.toggleFavorite(currentItem.id, currentFav)
                .onFailure {
                    // Revert on failure
                    _uiState.update { it.copy(isFavorite = currentFav) }
                }
        }
    }

    fun selectSeason(seasonId: String) {
        val series = _uiState.value.item ?: return
        _uiState.update { it.copy(selectedSeasonId = seasonId, isLoadingEpisodes = true) }
        viewModelScope.launch {
            repository.getEpisodes(series.id, seasonId)
                .onSuccess { eps ->
                    _uiState.update { it.copy(episodes = eps, isLoadingEpisodes = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingEpisodes = false) }
                }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { UiState(isLoading = true) }
            repository.getItemDetails(itemId)
                .onSuccess { item ->
                    val isFav = item.userData?.isFavorite ?: false
                    _uiState.update { it.copy(isLoading = false, item = item, isFavorite = isFav) }

                    if (item.type.equals("Series", ignoreCase = true)) {
                        loadSeriesData(item.id)
                    }
                }
                .onFailure { e ->
                    if (e is UnauthorizedException) {
                        _uiState.update { it.copy(isLoading = false, sessionExpired = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load details") }
                    }
                }
        }
    }

    private fun loadSeriesData(seriesId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEpisodes = true) }
            
            // Load seasons
            val seasonsResult = repository.getSeasons(seriesId)
            val seasons = seasonsResult.getOrDefault(emptyList())
            val firstSeasonId = seasons.firstOrNull()?.id

            // Load episodes
            val episodesResult = repository.getEpisodes(seriesId, firstSeasonId)
            val episodes = episodesResult.getOrDefault(emptyList())

            // Determine next episode to play (first unwatched or in-progress)
            val nextEp = episodes.firstOrNull { it.userData?.played != true } ?: episodes.firstOrNull()

            _uiState.update {
                it.copy(
                    seasons = seasons,
                    selectedSeasonId = firstSeasonId,
                    episodes = episodes,
                    nextEpisodeToPlay = nextEp,
                    isLoadingEpisodes = false
                )
            }
        }
    }
}

