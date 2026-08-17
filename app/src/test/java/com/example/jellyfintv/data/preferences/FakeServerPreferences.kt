package com.example.jellyfintv.data.preferences

/** In-memory [ServerPreferences] for tests - no Android [android.content.Context] required. */
class FakeServerPreferences : ServerPreferences {
    override var serverUrl: String = ""
    override var token: String = ""
    override var userId: String = ""
    override var username: String = ""
    override var trustSelfSignedCerts: Boolean = false

    override val isLoggedIn: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank() && userId.isNotBlank()

    override fun clear() {
        serverUrl = ""
        token = ""
        userId = ""
        username = ""
        trustSelfSignedCerts = false
    }
}
