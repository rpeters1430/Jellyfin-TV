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
        val parentSeries: MediaItem? = null,
        val seasons: List<MediaItem> = emptyList(),
        val selectedSeasonId: String? = null,
        val episodes: List<MediaItem> = emptyList(),
        val isLoadingEpisodes: Boolean = false,
        val nextEpisodeToPlay: MediaItem? = null,
        val selectedEpisodeForDetail: MediaItem? = null,
        val siblingEpisodes: List<MediaItem> = emptyList(),
        val playlistItems: List<MediaItem> = emptyList(),
        val isLoadingPlaylist: Boolean = false,
        val nextPlaylistItemToPlay: MediaItem? = null,
        val similarItems: List<MediaItem> = emptyList(),
        val isLoadingSimilar: Boolean = false,
        val isFavorite: Boolean = false,
        val isPlayed: Boolean = false,
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
    fun getBackdropUrl(id: String, index: Int = 0): String = repository.getBackdropUrl(id, index)
    fun getLogoUrl(id: String): String = repository.getLogoUrl(id)
    fun getPersonImageUrl(personId: String?, personName: String?): String = repository.getPersonImageUrl(personId, personName)
    fun getImageHeaders(): Map<String, String> = repository.getStreamHeaders()

    fun showEpisodeDetail(episode: MediaItem?) {
        _uiState.update { it.copy(selectedEpisodeForDetail = episode) }
    }

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

    fun togglePlayed() {
        val currentItem = _uiState.value.item ?: return
        val currentlyPlayed = _uiState.value.isPlayed
        _uiState.update { it.copy(isPlayed = !currentlyPlayed) }
        viewModelScope.launch {
            repository.togglePlayed(currentItem.id, currentlyPlayed)
                .onFailure {
                    // Revert on failure
                    _uiState.update { it.copy(isPlayed = currentlyPlayed) }
                }
        }
    }

    fun toggleEpisodePlayed(episode: MediaItem) {
        val currentlyPlayed = episode.userData?.played == true
        val newUserData = (episode.userData ?: com.example.jellyfintv.data.model.UserData()).copy(
            played = !currentlyPlayed,
            playedPercentage = if (!currentlyPlayed) 100f else 0f
        )
        val updatedEp = episode.copy(userData = newUserData)

        _uiState.update { current ->
            val updatedEpisodes = current.episodes.map { if (it.id == episode.id) updatedEp else it }
            val updatedSibling = current.siblingEpisodes.map { if (it.id == episode.id) updatedEp else it }
            val updatedItem = if (current.item?.id == episode.id) updatedEp else current.item
            val updatedSelected = if (current.selectedEpisodeForDetail?.id == episode.id) updatedEp else current.selectedEpisodeForDetail
            current.copy(
                episodes = updatedEpisodes,
                siblingEpisodes = updatedSibling,
                item = updatedItem,
                selectedEpisodeForDetail = updatedSelected,
                isPlayed = if (current.item?.id == episode.id) !currentlyPlayed else current.isPlayed
            )
        }
        viewModelScope.launch {
            repository.togglePlayed(episode.id, currentlyPlayed)
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
                    val isPlayed = item.userData?.played ?: false
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            item = item,
                            isFavorite = isFav,
                            isPlayed = isPlayed
                        )
                    }

                    loadSimilarItems(item.id)

                    if (item.type.equals("Series", ignoreCase = true)) {
                        loadSeriesData(item.id)
                    } else if (item.type.equals("Episode", ignoreCase = true)) {
                        loadEpisodeParentAndSiblings(item)
                    } else if (item.type.equals("Playlist", ignoreCase = true)) {
                        loadPlaylistData(item.id)
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

    private fun loadSimilarItems(targetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSimilar = true) }
            repository.getSimilarItems(targetId)
                .onSuccess { items ->
                    _uiState.update { it.copy(similarItems = items, isLoadingSimilar = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSimilar = false) }
                }
        }
    }

    private fun loadEpisodeParentAndSiblings(episode: MediaItem) {
        val seriesId = episode.seriesId ?: return
        viewModelScope.launch {
            repository.getItemDetails(seriesId).onSuccess { series ->
                _uiState.update { it.copy(parentSeries = series) }
            }
            repository.getEpisodes(seriesId, episode.seasonId).onSuccess { siblingList ->
                _uiState.update { it.copy(siblingEpisodes = siblingList) }
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

    private fun loadPlaylistData(playlistId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylist = true) }
            repository.getPlaylistItems(playlistId)
                .onSuccess { items ->
                    val nextItem = items.firstOrNull { it.userData?.played != true } ?: items.firstOrNull()
                    _uiState.update {
                        it.copy(
                            playlistItems = items,
                            nextPlaylistItemToPlay = nextItem,
                            isLoadingPlaylist = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingPlaylist = false) }
                }
        }
    }
}

