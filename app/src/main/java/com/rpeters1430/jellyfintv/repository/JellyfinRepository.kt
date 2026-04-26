package com.rpeters1430.jellyfintv.repository

import com.rpeters1430.jellyfintv.api.ApiClient
import com.rpeters1430.jellyfintv.models.*
import com.rpeters1430.jellyfintv.utils.PreferenceManager

/**
 * Single source of truth for all Jellyfin API calls.
 * Returns [Result] wrappers so ViewModels can handle success/failure uniformly.
 */
class JellyfinRepository(private val prefs: PreferenceManager) {

    private val api get() = requireNotNull(ApiClient.service) {
        "ApiClient has not been initialised. Call ApiClient.init() first."
    }

    // ── Auth ───────────────────────────────────────────────────────────────

    suspend fun checkServer(serverUrl: String): Result<PublicSystemInfo> = runCatching {
        val response = api.getPublicSystemInfo()
        response.body() ?: error("Empty response from server")
    }

    suspend fun login(username: String, password: String): Result<AuthenticationResult> =
        runCatching {
            val response = api.authenticateByName(
                AuthenticateUserByNameRequest(username, password)
            )
            if (response.isSuccessful) {
                response.body() ?: error("Empty auth response")
            } else {
                val code = response.code()
                val msg = response.errorBody()?.string() ?: "HTTP $code"
                error(msg)
            }
        }

    // ── Libraries ──────────────────────────────────────────────────────────

    suspend fun getLibraries(): Result<List<BaseItemDto>> = runCatching {
        val userId = prefs.userId ?: error("Not logged in")
        val response = api.getLibraries(userId)
        if (response.isSuccessful) {
            response.body()?.items ?: emptyList()
        } else {
            error("HTTP ${response.code()}")
        }
    }

    // ── Items ──────────────────────────────────────────────────────────────

    suspend fun getItems(
        parentId: String? = null,
        includeItemTypes: String? = null,
        limit: Int = 50,
        startIndex: Int = 0
    ): Result<BaseItemDtoQueryResult> = runCatching {
        val userId = prefs.userId ?: error("Not logged in")
        val response = api.getItems(
            userId = userId,
            parentId = parentId,
            includeItemTypes = includeItemTypes,
            limit = limit,
            startIndex = startIndex
        )
        if (response.isSuccessful) {
            response.body() ?: error("Empty items response")
        } else {
            error("HTTP ${response.code()}")
        }
    }

    suspend fun getLatestItems(
        parentId: String? = null,
        includeItemTypes: String? = null,
        limit: Int = 16
    ): Result<List<BaseItemDto>> = runCatching {
        val userId = prefs.userId ?: error("Not logged in")
        val response = api.getLatestItems(
            userId = userId,
            parentId = parentId,
            includeItemTypes = includeItemTypes,
            limit = limit
        )
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            error("HTTP ${response.code()}")
        }
    }

    suspend fun searchItems(query: String): Result<List<BaseItemDto>> = runCatching {
        val userId = prefs.userId ?: error("Not logged in")
        val response = api.searchItems(userId = userId, searchTerm = query)
        if (response.isSuccessful) {
            response.body()?.items ?: emptyList()
        } else {
            error("HTTP ${response.code()}")
        }
    }

    // ── Playback ───────────────────────────────────────────────────────────

    suspend fun getPlaybackInfo(itemId: String): Result<PlaybackInfoResponse> = runCatching {
        val userId = prefs.userId ?: error("Not logged in")
        val response = api.getPlaybackInfo(
            itemId = itemId,
            userId = userId,
            request = PlaybackInfoRequest(userId = userId)
        )
        if (response.isSuccessful) {
            response.body() ?: error("Empty playback info response")
        } else {
            error("HTTP ${response.code()}")
        }
    }

    suspend fun reportPlaybackProgress(
        itemId: String,
        positionTicks: Long,
        playSessionId: String? = null
    ) {
        val body = buildMap<String, Any> {
            put("ItemId", itemId)
            put("PositionTicks", positionTicks)
            if (playSessionId != null) put("PlaySessionId", playSessionId)
        }
        runCatching { api.reportPlaybackProgress(body) }
    }

    suspend fun reportPlaybackStopped(
        itemId: String,
        positionTicks: Long,
        playSessionId: String? = null
    ) {
        val body = buildMap<String, Any> {
            put("ItemId", itemId)
            put("PositionTicks", positionTicks)
            if (playSessionId != null) put("PlaySessionId", playSessionId)
        }
        runCatching { api.reportPlaybackStopped(body) }
    }
}
