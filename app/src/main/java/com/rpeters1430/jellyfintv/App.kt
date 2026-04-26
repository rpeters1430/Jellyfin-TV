package com.rpeters1430.jellyfintv

import android.app.Application
import com.rpeters1430.jellyfintv.api.ApiClient
import com.rpeters1430.jellyfintv.repository.JellyfinRepository
import com.rpeters1430.jellyfintv.utils.PreferenceManager

/**
 * Application class. Provides lazily-initialised singletons used throughout the app.
 */
class App : Application() {

    val prefs: PreferenceManager by lazy { PreferenceManager(applicationContext) }
    val repository: JellyfinRepository by lazy { JellyfinRepository(prefs) }

    override fun onCreate() {
        super.onCreate()
        // If a previous session exists, restore the API client so the rest of the
        // app can make authenticated requests immediately.
        val savedUrl = prefs.serverUrl
        if (!savedUrl.isNullOrBlank()) {
            ApiClient.init(
                baseUrl = savedUrl,
                tokenProvider = { prefs.accessToken },
                deviceId = prefs.deviceId
            )
        }
    }
}
