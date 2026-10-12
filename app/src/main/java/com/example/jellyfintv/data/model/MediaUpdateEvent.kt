package com.example.jellyfintv.data.model

sealed class MediaUpdateEvent {
    data class UserDataChanged(
        val userId: String? = null,
        val itemIds: List<String> = emptyList()
    ) : MediaUpdateEvent()

    data class LibraryChanged(
        val itemsAdded: List<String> = emptyList(),
        val itemsUpdated: List<String> = emptyList(),
        val itemsRemoved: List<String> = emptyList()
    ) : MediaUpdateEvent()

    data class PlaybackStopped(
        val itemId: String
    ) : MediaUpdateEvent()

    object RefreshRequested : MediaUpdateEvent()
}
