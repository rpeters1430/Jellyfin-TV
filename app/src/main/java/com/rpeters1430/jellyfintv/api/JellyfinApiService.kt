package com.rpeters1430.jellyfintv.api

import com.rpeters1430.jellyfintv.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the Jellyfin REST API.
 * All endpoints that require authentication expect an
 * `X-MediaBrowser-Token` header injected by [AuthInterceptor].
 */
interface JellyfinApiService {

    // ── System ─────────────────────────────────────────────────────────────

    /** Check server reachability and get basic info (no auth required). */
    @GET("System/Info/Public")
    suspend fun getPublicSystemInfo(): Response<PublicSystemInfo>

    // ── Auth ───────────────────────────────────────────────────────────────

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body request: com.rpeters1430.jellyfintv.models.AuthenticateUserByNameRequest
    ): Response<AuthenticationResult>

    // ── Libraries ──────────────────────────────────────────────────────────

    /** Returns the top-level media libraries for the current user. */
    @GET("Users/{userId}/Views")
    suspend fun getLibraries(
        @Path("userId") userId: String
    ): Response<BaseItemDtoQueryResult>

    // ── Items ──────────────────────────────────────────────────────────────

    /** Returns items inside a library/folder. */
    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,Overview,Genres,Studios,People",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("Limit") limit: Int = 50,
        @Query("StartIndex") startIndex: Int = 0
    ): Response<BaseItemDtoQueryResult>

    /** Returns the latest (recently-added) items in the library. */
    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,Overview",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("Limit") limit: Int = 16
    ): Response<List<BaseItemDto>>

    // ── Search ─────────────────────────────────────────────────────────────

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series,Episode",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,Overview",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("Limit") limit: Int = 50
    ): Response<BaseItemDtoQueryResult>

    // ── Playback ───────────────────────────────────────────────────────────

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Query("userId") userId: String,
        @Body request: PlaybackInfoRequest
    ): Response<PlaybackInfoResponse>

    /** Report playback progress to the server. */
    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    /** Report playback stopped. */
    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>
}
