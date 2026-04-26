package com.rpeters1430.jellyfintv.models

import com.google.gson.annotations.SerializedName

/** Represents a media item returned by the Jellyfin API. */
data class BaseItemDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String?,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("ProductionYear") val productionYear: Int?,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long?,
    @SerializedName("CommunityRating") val communityRating: Float?,
    @SerializedName("ImageTags") val imageTags: Map<String, String>?,
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String>?,
    @SerializedName("CollectionType") val collectionType: String?,
    @SerializedName("MediaType") val mediaType: String?,
    @SerializedName("SeriesId") val seriesId: String?,
    @SerializedName("SeriesName") val seriesName: String?,
    @SerializedName("ParentId") val parentId: String?,
    @SerializedName("IndexNumber") val indexNumber: Int?,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int?,
    @SerializedName("VideoType") val videoType: String?,
    @SerializedName("IsFolder") val isFolder: Boolean?,
    @SerializedName("Genres") val genres: List<String>?,
    @SerializedName("Studios") val studios: List<NameGuidPair>?,
    @SerializedName("People") val people: List<BaseItemPerson>?,
    @SerializedName("UserData") val userData: UserItemDataDto?
) {
    /** Duration in minutes derived from RunTimeTicks (1 tick = 100 nanoseconds). */
    val durationMinutes: Int?
        get() = runTimeTicks?.let { (it / 600_000_000).toInt() }
}

data class NameGuidPair(
    @SerializedName("Name") val name: String,
    @SerializedName("Id") val id: String
)

data class BaseItemPerson(
    @SerializedName("Name") val name: String,
    @SerializedName("Role") val role: String?,
    @SerializedName("Type") val type: String?
)

data class UserItemDataDto(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long?,
    @SerializedName("Played") val played: Boolean?,
    @SerializedName("IsFavorite") val isFavorite: Boolean?
)

data class BaseItemDtoQueryResult(
    @SerializedName("Items") val items: List<BaseItemDto>,
    @SerializedName("TotalRecordCount") val totalRecordCount: Int,
    @SerializedName("StartIndex") val startIndex: Int
)
