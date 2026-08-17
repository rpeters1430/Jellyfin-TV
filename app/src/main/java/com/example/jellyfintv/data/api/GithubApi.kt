package com.example.jellyfintv.data.api

import com.example.jellyfintv.data.model.GithubRelease
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

interface GithubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRelease>
}

/**
 * Deliberately separate from RetrofitClient - that one is keyed by the user's configured
 * Jellyfin server URL/TLS-trust setting, and mixing GitHub's fixed, always-valid-cert API into
 * that cache would conflate two unrelated concerns.
 */
object GithubClient {
    private val instance: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    // GitHub's REST API rejects requests with no User-Agent header (403),
                    // and OkHttp doesn't set one by default.
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "JellyfinTV-Android-App")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java)
    }

    fun getApi(): GithubApi = instance
}
