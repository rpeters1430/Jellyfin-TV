package com.rpeters1430.jellyfintv.models

import com.google.gson.annotations.SerializedName

data class PublicSystemInfo(
    @SerializedName("ServerName") val serverName: String?,
    @SerializedName("Version") val version: String?,
    @SerializedName("Id") val id: String?,
    @SerializedName("OperatingSystem") val operatingSystem: String?
)

data class MediaSourceInfo(
    @SerializedName("Id") val id: String?,
    @SerializedName("Path") val path: String?,
    @SerializedName("Protocol") val protocol: String?,
    @SerializedName("DirectStreamUrl") val directStreamUrl: String?,
    @SerializedName("SupportsDirectStream") val supportsDirectStream: Boolean?,
    @SerializedName("SupportsDirectPlay") val supportsDirectPlay: Boolean?,
    @SerializedName("Container") val container: String?
)

data class PlaybackInfoRequest(
    @SerializedName("UserId") val userId: String?,
    @SerializedName("MaxStreamingBitrate") val maxStreamingBitrate: Int? = 140_000_000
)

data class PlaybackInfoResponse(
    @SerializedName("MediaSources") val mediaSources: List<MediaSourceInfo>?,
    @SerializedName("PlaySessionId") val playSessionId: String?
)
