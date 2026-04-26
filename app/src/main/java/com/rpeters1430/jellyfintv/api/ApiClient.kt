package com.rpeters1430.jellyfintv.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton factory that builds and caches a [JellyfinApiService] for a given server URL.
 * Call [init] whenever the server URL or token changes (e.g. after login).
 */
object ApiClient {

    private var _service: JellyfinApiService? = null

    /** The currently configured service, or null if [init] hasn't been called yet. */
    val service: JellyfinApiService?
        get() = _service

    /**
     * (Re-)initialise the HTTP client for [baseUrl].
     *
     * @param baseUrl       Full server URL including trailing slash, e.g. "http://192.168.1.100:8096/"
     * @param tokenProvider Lambda that returns the current access token (or null if not authenticated).
     * @param deviceId      Stable identifier for this device.
     */
    fun init(
        baseUrl: String,
        tokenProvider: () -> String?,
        deviceId: String
    ) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider, deviceId))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        _service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JellyfinApiService::class.java)
    }
}
