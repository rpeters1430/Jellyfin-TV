package com.example.jellyfintv.data.repository

import com.example.jellyfintv.data.api.RetrofitClient
import com.example.jellyfintv.data.model.*
import com.example.jellyfintv.data.preferences.ServerPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.UUID

class UnauthorizedException(message: String = "Session expired, please sign in again") : Exception(message)

class JellyfinRepository(val prefs: ServerPreferences) {

    private val deviceId: String by lazy {
        UUID.nameUUIDFromBytes("JellyfinTVApp".toByteArray()).toString()
    }

    private fun getAuthHeader(): String {
        val base = "MediaBrowser Client=\"Jellyfin TV\", Device=\"Android TV\", DeviceId=\"$deviceId\", Version=\"1.0.0\""
        return if (prefs.token.isNotBlank()) {
            "$base, Token=\"${prefs.token}\""
        } else {
            base
        }
    }

    val isDemoMode: Boolean
        get() = prefs.serverUrl == "DEMO"

    /** Client for the currently connected server, honoring its saved self-signed-cert opt-in. */
    private fun api() = RetrofitClient.getApi(prefs.serverUrl, prefs.trustSelfSignedCerts)

    suspend fun authenticate(
        serverUrlInput: String,
        username: String,
        pass: String,
        trustSelfSignedCerts: Boolean = false
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        val raw = serverUrlInput.trim()
        if (raw.lowercase() == "demo") {
            prefs.serverUrl = "DEMO"
            prefs.token = "demo-token"
            prefs.userId = "demo-user-id"
            prefs.username = "Demo User"
            return@withContext Result.success(
                AuthResult(
                    accessToken = "demo-token",
                    user = UserInfo("demo-user-id", "Demo User"),
                    serverId = "demo-server"
                )
            )
        }

        val candidateUrls = mutableListOf<String>()

        var clean = raw.removeSuffix("/")
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            candidateUrls.add(clean)
        } else {
            // If domain contains .ts.net or no explicit scheme, default to https:// first, then http://
            if (clean.contains(".ts.net")) {
                candidateUrls.add("https://$clean")
                candidateUrls.add("http://$clean")
            } else {
                candidateUrls.add("https://$clean")
                candidateUrls.add("http://$clean")
                if (!clean.contains(":")) {
                    candidateUrls.add("http://$clean:8096")
                    candidateUrls.add("https://$clean:8920")
                }
            }
        }

        var lastException: Exception? = null

        for (candidate in candidateUrls) {
            try {
                val api = RetrofitClient.getApi(candidate, trustSelfSignedCerts)
                val response = api.authenticateByName(
                    authHeader = getAuthHeader(),
                    request = AuthRequest(username = username, pw = pass, password = pass)
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResult = response.body()!!
                    prefs.serverUrl = candidate
                    prefs.trustSelfSignedCerts = trustSelfSignedCerts
                    prefs.token = authResult.accessToken ?: ""
                    prefs.userId = authResult.user?.id ?: ""
                    prefs.username = authResult.user?.name ?: username
                    return@withContext Result.success(authResult)
                } else if (response.code() == 401) {
                    return@withContext Result.failure(Exception("Invalid username or password (401 Unauthorized)"))
                } else {
                    lastException = Exception("HTTP ${response.code()}: Connection failed on $candidate")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        val localized = lastException?.localizedMessage ?: lastException?.message ?: "Network error"
        Result.failure(Exception(localized, lastException))
    }

    fun logout() {
        prefs.clear()
    }

    fun getPosterUrl(itemId: String): String {
        if (isDemoMode) {
            return when (itemId) {
                "mock-1" -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80"
                "mock-2" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80"
                "mock-3" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
                "mock-4" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
                "mock-series-1", "mock-season-1" -> "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80"
                "mock-season-2" -> "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"
                "mock-series-2", "mock-season-2-1", "mock-season-2-2" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
                "mock-ep-101" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
                "mock-ep-102" -> "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"
                "mock-ep-103" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
                "mock-ep-104" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80"
                "mock-ep-105" -> "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80"
                "mock-ep-106" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80"
                "mock-ep-107" -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80"
                "mock-ep-201", "mock-ep-202", "mock-ep-203", "mock-ep-204" -> "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&auto=format&fit=crop&q=80"
                "mock-music-1" -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                "mock-music-2" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
                "mock-video-1" -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80"
                "mock-video-2" -> "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80"
                "mock-playlist-1", "mock-pl-item-101", "mock-pl-item-102", "mock-pl-item-103", "mock-pl-item-104" -> "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80"
                "mock-playlist-2", "mock-pl-item-201", "mock-pl-item-202", "mock-pl-item-203" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
                "mock-playlist-3", "mock-pl-item-301", "mock-pl-item-302", "mock-pl-item-303" -> "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&auto=format&fit=crop&q=80"
                else -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80"
            }
        }
        return "${prefs.serverUrl}/Items/$itemId/Images/Primary?fillWidth=400&fillHeight=600&quality=90"
    }

    fun getBackdropUrl(itemId: String, index: Int = 0): String {
        if (isDemoMode) {
            return when (itemId) {
                "mock-1" -> if (index == 1) "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1920&auto=format&fit=crop&q=80" else "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&auto=format&fit=crop&q=80"
                "mock-2" -> if (index == 1) "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920&auto=format&fit=crop&q=80" else "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1920&auto=format&fit=crop&q=80"
                "mock-3" -> "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1920&auto=format&fit=crop&q=80"
                "mock-4" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920&auto=format&fit=crop&q=80"
                "mock-series-1", "mock-season-1", "mock-season-2" -> "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1920&auto=format&fit=crop&q=80"
                "mock-series-2", "mock-season-2-1", "mock-season-2-2" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-101" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-102" -> "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-103" -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-104" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-105" -> "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-106" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-107" -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1920&auto=format&fit=crop&q=80"
                "mock-ep-201", "mock-ep-202", "mock-ep-203", "mock-ep-204" -> "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1920&auto=format&fit=crop&q=80"
                "mock-music-1" -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1920&auto=format&fit=crop&q=80"
                "mock-music-2" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=1920&auto=format&fit=crop&q=80"
                "mock-video-1" -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920&auto=format&fit=crop&q=80"
                "mock-video-2" -> "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&auto=format&fit=crop&q=80"
                "mock-playlist-1", "mock-pl-item-101", "mock-pl-item-102", "mock-pl-item-103", "mock-pl-item-104" -> "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=1920&auto=format&fit=crop&q=80"
                "mock-playlist-2", "mock-pl-item-201", "mock-pl-item-202", "mock-pl-item-203" -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&auto=format&fit=crop&q=80"
                "mock-playlist-3", "mock-pl-item-301", "mock-pl-item-302", "mock-pl-item-303" -> "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=1920&auto=format&fit=crop&q=80"
                else -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&auto=format&fit=crop&q=80"
            }
        }
        return "${prefs.serverUrl}/Items/$itemId/Images/Backdrop/$index?maxWidth=1920&quality=85"
    }

    fun getLogoUrl(itemId: String): String {
        if (isDemoMode) {
            return ""
        }
        return "${prefs.serverUrl}/Items/$itemId/Images/Logo?maxWidth=800&quality=90"
    }

    fun getPersonImageUrl(personId: String?, personName: String?): String {
        if (isDemoMode) {
            return "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80"
        }
        if (!personId.isNullOrBlank()) {
            return "${prefs.serverUrl}/Items/$personId/Images/Primary?maxWidth=400&quality=85"
        }
        val encoded = personName?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
        return "${prefs.serverUrl}/Persons/$encoded/Images/Primary?maxWidth=400&quality=85"
    }

    fun getStreamUrl(itemId: String, mediaSourceId: String? = null): String {
        if (isDemoMode) {
            return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }
        val source = mediaSourceId ?: itemId
        return "${prefs.serverUrl}/Videos/$itemId/stream?static=true&MediaSourceId=$source"
    }

    /**
     * Auth headers for raw HTTP media requests that bypass Retrofit (so there's no automatic
     * @Header injection): the video stream itself, plus poster/backdrop image loads via Coil.
     * Used instead of an api_key query param so the token never ends up in a URL - and
     * therefore never in the request-line logging RetrofitClient's OkHttpClient does.
     */
    fun getStreamHeaders(): Map<String, String> {
        if (isDemoMode) return emptyMap()
        return mapOf("Authorization" to getAuthHeader())
    }

    private fun <T> Response<T>.toResult(notFoundMessage: String): Result<T> {
        val responseBody = body()
        return when {
            isSuccessful && responseBody != null -> Result.success(responseBody)
            code() == 401 -> Result.failure(UnauthorizedException())
            else -> Result.failure(Exception("$notFoundMessage (HTTP ${code()})"))
        }
    }

    suspend fun getResumeItems(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(SampleMediaData.sampleMovies.filter { it.userData?.playedPercentage ?: 0f > 0f })
        }
        try {
            val api = api()
            api.getResumeItems(getAuthHeader(), prefs.userId)
                .toResult("Could not load continue watching")
                .map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNextUpItems(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(SampleMediaData.sampleSeries)
        }
        try {
            val api = api()
            api.getNextUp(getAuthHeader(), prefs.userId)
                .toResult("Could not load next up")
                .map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestMovies(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(SampleMediaData.sampleMovies)
        }
        try {
            val api = api()
            api.getItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                includeItemTypes = "Movie",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = 16
            ).toResult("Could not load latest movies").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestSeries(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(SampleMediaData.sampleSeries)
        }
        try {
            val api = api()
            api.getItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                includeItemTypes = "Series",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = 16
            ).toResult("Could not load latest series").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserViews(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(
                listOf(
                    MediaItem(id = "view-movies", name = "Movies", type = "CollectionFolder", collectionType = "movies"),
                    MediaItem(id = "view-series", name = "TV Shows", type = "CollectionFolder", collectionType = "tvshows"),
                    MediaItem(id = "view-playlists", name = "Playlists", type = "CollectionFolder", collectionType = "playlists"),
                    MediaItem(id = "view-music", name = "Music", type = "CollectionFolder", collectionType = "music"),
                    MediaItem(id = "view-stuff", name = "Stuff (Home Videos & Photos)", type = "CollectionFolder", collectionType = "homevideos")
                )
            )
        }
        try {
            val api = api()
            api.getUserViews(getAuthHeader(), prefs.userId)
                .toResult("Could not load user libraries")
                .map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestItemsByParent(parentId: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val items = when (parentId) {
                "view-playlists" -> SampleMediaData.samplePlaylists
                "view-music" -> SampleMediaData.sampleMusic
                "view-stuff" -> SampleMediaData.sampleHomeVideos
                "view-series" -> SampleMediaData.sampleSeries
                else -> SampleMediaData.sampleMovies
            }
            return@withContext Result.success(items)
        }
        try {
            val api = api()
            api.getLatestItems(getAuthHeader(), prefs.userId, parentId = parentId, limit = 16)
                .toResult("Could not load latest items for library")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemsByParent(
        parentId: String,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
        limit: Int = 60
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val items = when (parentId) {
                "view-playlists" -> SampleMediaData.samplePlaylists
                "view-music" -> SampleMediaData.sampleMusic
                "view-stuff" -> SampleMediaData.sampleHomeVideos
                "view-series" -> SampleMediaData.sampleSeries
                else -> SampleMediaData.sampleMovies
            }
            return@withContext Result.success(items)
        }
        try {
            val api = api()
            api.getItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                parentId = parentId,
                recursive = true,
                sortBy = sortBy,
                sortOrder = sortOrder,
                limit = limit
            ).toResult("Could not load library items").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemDetails(itemId: String): Result<MediaItem> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val allMock = SampleMediaData.sampleMovies + SampleMediaData.sampleSeries +
                SampleMediaData.sampleSeasons.values.flatten() +
                SampleMediaData.sampleEpisodes.values.flatten() +
                SampleMediaData.samplePlaylists + SampleMediaData.samplePlaylistItems.values.flatten() +
                SampleMediaData.sampleMusic + SampleMediaData.sampleHomeVideos
            val item = allMock.find { it.id == itemId } ?: SampleMediaData.sampleMovies.first()
            return@withContext Result.success(item)
        }
        try {
            val api = api()
            api.getItemDetails(getAuthHeader(), prefs.userId, itemId)
                .toResult("Could not fetch item details")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistItems(playlistId: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val items = SampleMediaData.samplePlaylistItems[playlistId]
                ?: SampleMediaData.samplePlaylistItems["mock-playlist-1"]
                ?: emptyList()
            return@withContext Result.success(items)
        }
        try {
            val api = api()
            val result = api.getPlaylistItems(getAuthHeader(), playlistId, prefs.userId)
                .toResult("Could not load playlist items")
                .map { it.items }

            // If the primary playlist items endpoint succeeded and returned items, return them
            if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                return@withContext result
            }

            // Fallback: Query items with parentId=playlistId (covers plugins/folders categorized as playlists)
            val fallback = api.getItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                parentId = playlistId,
                recursive = true,
                sortBy = "SortName",
                sortOrder = "Ascending",
                limit = 100
            ).toResult("Could not load playlist items").map { it.items }

            if (fallback.isSuccess) fallback else result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeasons(seriesId: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val seasons = SampleMediaData.sampleSeasons[seriesId]
                ?: SampleMediaData.sampleSeasons["mock-series-1"]
                ?: emptyList()
            return@withContext Result.success(seasons)
        }
        try {
            val api = api()
            api.getSeasons(getAuthHeader(), seriesId, prefs.userId)
                .toResult("Could not load seasons")
                .map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodes(seriesId: String, seasonId: String? = null): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val allEps = SampleMediaData.sampleEpisodes[seriesId]
                ?: SampleMediaData.sampleEpisodes.values.flatten()
            val filtered = if (seasonId != null) {
                allEps.filter { it.seasonId == seasonId }
            } else {
                allEps
            }
            return@withContext Result.success(filtered.ifEmpty { allEps })
        }
        try {
            val api = api()
            api.getEpisodes(
                authHeader = getAuthHeader(),
                seriesId = seriesId,
                seasonId = seasonId,
                userId = prefs.userId
            ).toResult("Could not load episodes").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMedia(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext Result.success(emptyList())
        if (isDemoMode) {
            val allMock = SampleMediaData.sampleMovies + SampleMediaData.sampleSeries +
                SampleMediaData.sampleEpisodes.values.flatten() +
                SampleMediaData.samplePlaylists +
                SampleMediaData.samplePlaylistItems.values.flatten() +
                SampleMediaData.sampleMusic +
                SampleMediaData.sampleHomeVideos
            val filtered = allMock.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                (it.overview?.contains(cleanQuery, ignoreCase = true) == true) ||
                (it.genres?.any { g -> g.contains(cleanQuery, ignoreCase = true) } == true) ||
                (it.seriesName?.contains(cleanQuery, ignoreCase = true) == true)
            }
            return@withContext Result.success(filtered)
        }
        try {
            val api = api()
            api.searchItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                searchTerm = cleanQuery
            ).toResult("Search failed").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(itemId: String, isFavorite: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        if (isDemoMode) return@withContext Result.success(!isFavorite)
        try {
            val api = api()
            val response = if (isFavorite) {
                api.unmarkFavorite(getAuthHeader(), itemId, prefs.userId)
            } else {
                api.markFavorite(getAuthHeader(), itemId, prefs.userId)
            }
            if (response.isSuccessful) {
                Result.success(!isFavorite)
            } else {
                Result.failure(Exception("Failed to update favorite status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSimilarItems(itemId: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            val current = (SampleMediaData.sampleMovies + SampleMediaData.sampleSeries).find { it.id == itemId }
            val currentGenres = current?.genres ?: emptyList()
            val similar = (SampleMediaData.sampleMovies + SampleMediaData.sampleSeries)
                .filter { it.id != itemId && (it.genres?.any { g -> currentGenres.contains(g) } == true || it.type == current?.type) }
                .ifEmpty { SampleMediaData.sampleMovies.filter { it.id != itemId } }
            return@withContext Result.success(similar)
        }
        try {
            val api = api()
            api.getSimilarItems(getAuthHeader(), itemId, prefs.userId)
                .toResult("Could not load similar items")
                .map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePlayed(itemId: String, currentlyPlayed: Boolean): Result<UserData> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext Result.success(
                UserData(
                    played = !currentlyPlayed,
                    playedPercentage = if (!currentlyPlayed) 100f else 0f
                )
            )
        }
        try {
            val api = api()
            val response = if (currentlyPlayed) {
                api.unmarkPlayed(getAuthHeader(), prefs.userId, itemId)
            } else {
                api.markPlayed(getAuthHeader(), prefs.userId, itemId)
            }
            response.toResult("Could not update watched status")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLibraryItems(
        includeItemTypes: String,
        sortBy: String = "SortName",
        sortOrder: String = "Ascending"
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext when {
                includeItemTypes.contains("Playlist", ignoreCase = true) -> Result.success(SampleMediaData.samplePlaylists)
                includeItemTypes.contains("Movie", ignoreCase = true) -> Result.success(SampleMediaData.sampleMovies)
                includeItemTypes.contains("Series", ignoreCase = true) -> Result.success(SampleMediaData.sampleSeries)
                includeItemTypes.contains("Music", ignoreCase = true) -> Result.success(SampleMediaData.sampleMusic)
                else -> Result.success(SampleMediaData.samplePlaylists)
            }
        }
        try {
            val api = api()
            api.getItems(
                authHeader = getAuthHeader(),
                userId = prefs.userId,
                includeItemTypes = includeItemTypes,
                sortBy = sortBy,
                sortOrder = sortOrder,
                limit = 60
            ).toResult("Could not load library").map { it.items }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportPlayingProgress(itemId: String, positionTicks: Long, isPaused: Boolean) = withContext(Dispatchers.IO) {
        if (isDemoMode) return@withContext
        try {
            val api = api()
            api.reportPlayingProgress(getAuthHeader(), PlaystateRequest(itemId = itemId, positionTicks = positionTicks, isPaused = isPaused))
        } catch (_: Exception) {}
    }

    suspend fun reportPlayingStopped(itemId: String, positionTicks: Long) = withContext(Dispatchers.IO) {
        if (isDemoMode) return@withContext
        try {
            val api = api()
            api.reportPlayingStopped(getAuthHeader(), PlaystateRequest(itemId = itemId, positionTicks = positionTicks))
        } catch (_: Exception) {}
    }
}
