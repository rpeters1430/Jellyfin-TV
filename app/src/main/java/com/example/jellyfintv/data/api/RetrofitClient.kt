package com.example.jellyfintv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Cache key includes trustSelfSignedCerts so flipping the setting can't reuse a
    // client built with the wrong trust behavior for a given server.
    private var currentCacheKey: String = ""
    private var currentApi: JellyfinApi? = null

    /**
     * Accepts any TLS certificate, including from an active man-in-the-middle, with no
     * verification at all. Only ever used when the user has explicitly opted in to
     * connecting to a server with a self-signed certificate.
     */
    private fun getTrustAllCertsClient(): OkHttpClient.Builder {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            OkHttpClient.Builder()
        }
    }

    /**
     * Shared OkHttpClient builder for both the JSON API and media streaming. Logging is
     * capped at BASIC (request line + response code only) so auth tokens and passwords -
     * which travel in headers/bodies, never in the request line - can't end up in logcat.
     */
    fun buildOkHttpClient(trustSelfSignedCerts: Boolean): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Default to standard system TLS trust-chain validation. Only bypass it when the
        // user has explicitly flagged this server as using a self-signed certificate.
        val clientBuilder = if (trustSelfSignedCerts) getTrustAllCertsClient() else OkHttpClient.Builder()

        return clientBuilder
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun getApi(baseUrl: String, trustSelfSignedCerts: Boolean = false): JellyfinApi {
        var formattedUrl = baseUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }

        val cacheKey = "$formattedUrl|$trustSelfSignedCerts"
        if (currentApi != null && currentCacheKey == cacheKey) {
            return currentApi!!
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(buildOkHttpClient(trustSelfSignedCerts))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        currentCacheKey = cacheKey
        currentApi = retrofit.create(JellyfinApi::class.java)
        return currentApi!!
    }
}
