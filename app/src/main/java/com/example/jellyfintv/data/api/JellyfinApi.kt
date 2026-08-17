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

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String
    ): Response<ViewsResponse>

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 12,
        @Query("fields") fields: String = "Overview,Genres,OfficialRating,CommunityRating,RunTimeTicks,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @GET("Shows/NextUp")
    suspend fun getNextUp(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 12,
        @Query("fields") fields: String = "Overview,Genres,OfficialRating,CommunityRating,RunTimeTicks"
    ): Response<ItemsResponse>

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("limit") limit: Int = 16,
        @Query("fields") fields: String = "Overview,Genres,OfficialRating,CommunityRating,RunTimeTicks"
    ): Response<List<MediaItem>>

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("includeItemTypes") includeItemTypes: String? = null, // "Movie", "Series"
        @Query("recursive") recursive: Boolean = true,
        @Query("sortBy") sortBy: String = "SortName",
        @Query("sortOrder") sortOrder: String = "Ascending",
        @Query("limit") limit: Int = 30,
        @Query("fields") fields: String = "Overview,Genres,OfficialRating,CommunityRating,RunTimeTicks,MediaSources"
    ): Response<ItemsResponse>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItemDetails(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<MediaItem>

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Header("Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String,
        @Query("fields") fields: String = "Overview,UserData"
    ): Response<ItemsResponse>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Header("Authorization") authHeader: String,
        @Path("seriesId") seriesId: String,
        @Query("seasonId") seasonId: String? = null,
        @Query("userId") userId: String,
        @Query("fields") fields: String = "Overview,RunTimeTicks,UserData,PrimaryImageAspectRatio"
    ): Response<ItemsResponse>

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Query("searchTerm") searchTerm: String,
        @Query("includeItemTypes") includeItemTypes: String? = "Movie,Series,Episode",
        @Query("recursive") recursive: Boolean = true,
        @Query("limit") limit: Int = 40,
        @Query("fields") fields: String = "Overview,Genres,OfficialRating,CommunityRating,RunTimeTicks,PrimaryImageAspectRatio"
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
