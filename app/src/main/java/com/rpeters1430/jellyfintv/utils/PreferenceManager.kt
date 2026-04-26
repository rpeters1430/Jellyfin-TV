package com.rpeters1430.jellyfintv.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Thin wrapper around SharedPreferences that stores Jellyfin session data.
 */
class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value) }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: generateDeviceId().also { deviceId = it }
        set(value) = prefs.edit { putString(KEY_DEVICE_ID, value) }

    /** True when the user has a stored access token from a previous login. */
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() && !serverUrl.isNullOrBlank()

    /** Clear all stored session data (logout). */
    fun logout() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
        }
    }

    private fun generateDeviceId(): String =
        java.util.UUID.randomUUID().toString().replace("-", "")

    companion object {
        private const val PREFS_NAME = "jellyfin_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
