package com.example.jellyfintv.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted connection state for the current Jellyfin server. An interface (rather than just
 * [AndroidServerPreferences]) so tests can substitute an in-memory fake instead of needing a
 * real Android [Context] / SharedPreferences.
 */
interface ServerPreferences {
    var serverUrl: String
    var token: String
    var userId: String
    var username: String

    // Opt-in only: bypasses TLS certificate validation entirely for this server.
    // Must never default to true - see RetrofitClient.getTrustAllCertsClient().
    var trustSelfSignedCerts: Boolean

    val isLoggedIn: Boolean

    fun clear()
}

class AndroidServerPreferences(context: Context) : ServerPreferences {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) {
            val cleanUrl = value.trim().removeSuffix("/")
            prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
        }

    override var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    override var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    override var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    override var trustSelfSignedCerts: Boolean
        get() = prefs.getBoolean(KEY_TRUST_SELF_SIGNED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRUST_SELF_SIGNED, value).apply()

    override val isLoggedIn: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank() && userId.isNotBlank()

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "jellyfin_tv_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_TRUST_SELF_SIGNED = "trust_self_signed_certs"
    }
}
