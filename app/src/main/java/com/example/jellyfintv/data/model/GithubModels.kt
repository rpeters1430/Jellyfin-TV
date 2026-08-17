package com.example.jellyfintv.data.model

import com.google.gson.annotations.SerializedName

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("draft") val draft: Boolean = false,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("assets") val assets: List<GithubAsset> = emptyList()
)

data class GithubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    @SerializedName("size") val size: Long = 0L
)
