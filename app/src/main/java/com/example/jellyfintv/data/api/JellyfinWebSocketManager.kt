package com.example.jellyfintv.data.api

import android.util.Log
import com.example.jellyfintv.data.model.MediaUpdateEvent
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.atomic.AtomicBoolean

class JellyfinWebSocketManager(
    private val okHttpClient: OkHttpClient,
    private val onEvent: (MediaUpdateEvent) -> Unit
) {
    private val tag = "JellyfinWebSocket"
    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    private var currentServerUrl: String = ""
    private var currentToken: String = ""
    private var currentDeviceId: String = ""
    private var retryDelayMs = 3_000L

    fun start(serverUrl: String, token: String, deviceId: String) {
        if (serverUrl.isBlank() || token.isBlank() || serverUrl.equals("DEMO", ignoreCase = true)) {
            return
        }

        currentServerUrl = serverUrl
        currentToken = token
        currentDeviceId = deviceId
        isRunning.set(true)
        retryDelayMs = 3_000L

        connect()
    }

    fun stop() {
        isRunning.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "App closed or logged out")
        } catch (_: Exception) {}
        webSocket = null
    }

    private fun buildSocketUrl(baseUrl: String, token: String, deviceId: String): String {
        val clean = baseUrl.trim().removeSuffix("/")
        val wsBase = when {
            clean.startsWith("https://", ignoreCase = true) -> "wss://" + clean.substring(8)
            clean.startsWith("http://", ignoreCase = true) -> "ws://" + clean.substring(7)
            else -> "ws://$clean"
        }
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val encodedDeviceId = java.net.URLEncoder.encode(deviceId, "UTF-8")
        return "$wsBase/socket?api_key=$encodedToken&deviceId=$encodedDeviceId"
    }

    private fun connect() {
        if (!isRunning.get()) return

        val socketUrl = buildSocketUrl(currentServerUrl, currentToken, currentDeviceId)
        val request = Request.Builder()
            .url(socketUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logD("Jellyfin WebSocket connected to $currentServerUrl")
                retryDelayMs = 3_000L
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logD("Jellyfin WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logD("Jellyfin WebSocket closed: $code / $reason")
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                logW("Jellyfin WebSocket error: ${t.message}")
                scheduleReconnect()
            }
        })
    }

    private fun logD(msg: String) {
        try { Log.d(tag, msg) } catch (_: Throwable) {}
    }

    private fun logW(msg: String) {
        try { Log.w(tag, msg) } catch (_: Throwable) {}
    }

    internal fun handleMessage(webSocket: WebSocket, text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val messageType = json.get("MessageType")?.asString ?: return

            when (messageType) {
                "ForceKeepAlive" -> {
                    webSocket.send("{\"MessageType\":\"KeepAlive\"}")
                }
                "UserDataChanged" -> {
                    val dataObj = json.getAsJsonObject("Data")
                    val userId = dataObj?.get("UserId")?.asString
                    val itemIds = mutableListOf<String>()
                    val userDataList = dataObj?.getAsJsonArray("UserDataList")
                    userDataList?.forEach { element ->
                        element.asJsonObject.get("ItemId")?.asString?.let { itemIds.add(it) }
                    }
                    onEvent(MediaUpdateEvent.UserDataChanged(userId = userId, itemIds = itemIds))
                }
                "LibraryChanged" -> {
                    val dataObj = json.getAsJsonObject("Data")
                    val added = dataObj?.getAsJsonArray("ItemsAdded")?.mapNotNull { it.asString } ?: emptyList()
                    val updated = dataObj?.getAsJsonArray("ItemsUpdated")?.mapNotNull { it.asString } ?: emptyList()
                    val removed = dataObj?.getAsJsonArray("ItemsRemoved")?.mapNotNull { it.asString } ?: emptyList()
                    onEvent(MediaUpdateEvent.LibraryChanged(itemsAdded = added, itemsUpdated = updated, itemsRemoved = removed))
                }
                "RestartRequired", "ServerShuttingDown" -> {
                    onEvent(MediaUpdateEvent.RefreshRequested)
                }
            }
        } catch (e: Exception) {
            logW("Failed to parse WebSocket message: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (!isRunning.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(30_000L)
            connect()
        }
    }
}
