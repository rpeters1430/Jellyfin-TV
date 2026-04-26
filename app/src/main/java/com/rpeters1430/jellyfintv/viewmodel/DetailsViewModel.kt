package com.rpeters1430.jellyfintv.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.models.PlaybackInfoResponse
import com.rpeters1430.jellyfintv.repository.JellyfinRepository
import kotlinx.coroutines.launch

class DetailsViewModel(private val repository: JellyfinRepository) : ViewModel() {

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Loading : PlaybackState()
        data class Ready(val playbackInfo: PlaybackInfoResponse, val item: BaseItemDto) : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }

    private val _playbackState = MutableLiveData<PlaybackState>(PlaybackState.Idle)
    val playbackState: LiveData<PlaybackState> = _playbackState

    fun requestPlayback(item: BaseItemDto) {
        _playbackState.value = PlaybackState.Loading
        viewModelScope.launch {
            repository.getPlaybackInfo(item.id).fold(
                onSuccess = { info ->
                    _playbackState.value = PlaybackState.Ready(info, item)
                },
                onFailure = { e ->
                    _playbackState.value = PlaybackState.Error(
                        e.message ?: "Failed to get playback info"
                    )
                }
            )
        }
    }

    fun resetPlaybackState() {
        _playbackState.value = PlaybackState.Idle
    }
}
