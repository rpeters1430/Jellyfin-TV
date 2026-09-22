package com.example.jellyfintv.data.api

import com.example.jellyfintv.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface JellyfinApi {

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("Authorization") authHeader: String,
        @Body request: AuthRequest
    ): Response<AuthResult>

    // Jellyfin 12 removed the old per-user "Users/{userId}/..." item routes (GetUserViews,
    // GetResumeItems, GetLatestMedia, GetItems, GetItem, MarkPlayed/Unplayed all moved to a
    // top-level route taking `userId` as a query parameter), so a request to the old paths 404s.
    // `fields` may only contain ItemFields enum names; OfficialRating/CommunityRating/RunTimeTicks/
    // UserData aren't in it (those are always returned), so don't list them.
    @GET("UserViews")
    suspend fun getUserViews(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String
    ): Response<ViewsResponse>

    @GET("UserItems/Resume")
    suspend fun getResumeItems(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 12,
        @Query("fields") fields: String = "Overview,Genres,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @GET("Shows/NextUp")
    suspend fun getNextUp(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 12,
        @Query("fields") fields: String = "Overview,Genres"
    ): Response<ItemsResponse>

    @GET("Items/Latest")
    suspend fun getLatestItems(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("limit") limit: Int = 16,
        @Query("fields") fields: String = "Overview,Genres"
    ): Response<List<MediaItem>>

    @GET("Items")
    suspend fun getItems(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("includeItemTypes") includeItemTypes: String? = null, // "Movie", "Series"
        @Query("recursive") recursive: Boolean = true,
        @Query("sortBy") sortBy: String = "SortName",
        @Query("sortOrder") sortOrder: String = "Ascending",
        @Query("limit") limit: Int = 30,
        @Query("fields") fields: String = "Overview,Genres,MediaSources"
    ): Response<ItemsResponse>

    @GET("Items/{itemId}")
    suspend fun getItemDetails(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String
    ): Response<MediaItem>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Header("Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("fields") fields: String = "Overview"
    ): Response<ItemsResponse>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Header("Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("seasonId") seasonId: String? = null,
        @Query("userId") userId: String,
        @Query("fields") fields: String = "Overview,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @GET("Playlists/{playlistId}/Items")
    suspend fun getPlaylistItems(
        @Header("Authorization") authHeader: String,
        @Path("playlistId") playlistId: String,
        @Query("userId") userId: String,
        @Query("fields") fields: String = "Overview,Genres,MediaSources,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @GET("Items")
    suspend fun searchItems(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("searchTerm") searchTerm: String,
        @Query("includeItemTypes") includeItemTypes: String? = "Movie,Series,Episode,Video,MusicAlbum,Audio,Playlist",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 40,
        @Query("fields") fields: String = "Overview,Genres,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @POST("UserFavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String
    ): Response<UserData>

    @DELETE("UserFavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String
    ): Response<UserData>

    @GET("Items/{itemId}/Similar")
    suspend fun getSimilarItems(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 12,
        @Query("fields") fields: String = "Overview,Genres,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @POST("UserPlayedItems/{itemId}")
    suspend fun markPlayed(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String
    ): Response<UserData>

    @DELETE("UserPlayedItems/{itemId}")
    suspend fun unmarkPlayed(
        @Header("Authorization") authHeader: String,
        @Path("itemId") itemId: String,
        @Query("userId") userId: String
    ): Response<UserData>

    @POST("Sessions/Playing")
    suspend fun reportPlayingStart(
        @Header("Authorization") authHeader: String,
        @Body request: PlaystateRequest
    ): Response<Unit>

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlayingProgress(
        @Header("Authorization") authHeader: String,
        @Body request: PlaystateRequest
    ): Response<Unit>

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlayingStopped(
        @Header("Authorization") authHeader: String,
        @Body request: PlaystateRequest
    ): Response<Unit>
}
