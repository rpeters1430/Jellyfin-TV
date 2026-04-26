package com.rpeters1430.jellyfintv.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches the required Jellyfin authentication headers
 * and the `Authorization` header required by newer Jellyfin server versions.
 *
 * Header format (Jellyfin MediaBrowser scheme):
 *   Authorization: MediaBrowser Client="Jellyfin TV", Device="AndroidTV",
 *                  DeviceId="<deviceId>", Version="1.0.0"[, Token="<accessToken>"]
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val deviceId: String,
    private val appVersion: String = "1.0.0"
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()

        val authHeader = buildString {
            append("MediaBrowser Client=\"Jellyfin TV\"")
            append(", Device=\"AndroidTV\"")
            append(", DeviceId=\"$deviceId\"")
            append(", Version=\"$appVersion\"")
            if (!token.isNullOrBlank()) {
                append(", Token=\"$token\"")
            }
        }

        val request = chain.request()
            .newBuilder()
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .build()

        return chain.proceed(request)
    }
}
