package com.example.jellyfintv.data.api

import com.example.jellyfintv.data.model.MediaUpdateEvent
import io.mockk.mockk
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinWebSocketManagerTest {

    @Test
    fun `ForceKeepAlive responds with KeepAlive`() {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val manager = JellyfinWebSocketManager(OkHttpClient()) { }

        manager.handleMessage(webSocket, """{"MessageType":"ForceKeepAlive"}""")

        verify { webSocket.send("""{"MessageType":"KeepAlive"}""") }
    }

    @Test
    fun `UserDataChanged parses item IDs and emits event`() {
        var receivedEvent: MediaUpdateEvent? = null
        val webSocket = mockk<WebSocket>(relaxed = true)
        val manager = JellyfinWebSocketManager(OkHttpClient()) { event ->
            receivedEvent = event
        }

        val json = """
        {
            "MessageType": "UserDataChanged",
            "Data": {
                "UserId": "user-42",
                "UserDataList": [
                    {"ItemId": "item-101", "Played": true},
                    {"ItemId": "item-102", "Played": false}
                ]
            }
        }
        """.trimIndent()

        manager.handleMessage(webSocket, json)

        assertTrue(receivedEvent is MediaUpdateEvent.UserDataChanged)
        val event = receivedEvent as MediaUpdateEvent.UserDataChanged
        assertEquals("user-42", event.userId)
        assertEquals(listOf("item-101", "item-102"), event.itemIds)
    }

    @Test
    fun `LibraryChanged parses added, updated, removed lists and emits event`() {
        var receivedEvent: MediaUpdateEvent? = null
        val webSocket = mockk<WebSocket>(relaxed = true)
        val manager = JellyfinWebSocketManager(OkHttpClient()) { event ->
            receivedEvent = event
        }

        val json = """
        {
            "MessageType": "LibraryChanged",
            "Data": {
                "ItemsAdded": ["lib-item-1"],
                "ItemsUpdated": ["lib-item-2"],
                "ItemsRemoved": []
            }
        }
        """.trimIndent()

        manager.handleMessage(webSocket, json)

        assertTrue(receivedEvent is MediaUpdateEvent.LibraryChanged)
        val event = receivedEvent as MediaUpdateEvent.LibraryChanged
        assertEquals(listOf("lib-item-1"), event.itemsAdded)
        assertEquals(listOf("lib-item-2"), event.itemsUpdated)
        assertEquals(emptyList<String>(), event.itemsRemoved)
    }

    @Test
    fun `RestartRequired emits RefreshRequested`() {
        var receivedEvent: MediaUpdateEvent? = null
        val webSocket = mockk<WebSocket>(relaxed = true)
        val manager = JellyfinWebSocketManager(OkHttpClient()) { event ->
            receivedEvent = event
        }

        manager.handleMessage(webSocket, """{"MessageType":"RestartRequired"}""")

        assertEquals(MediaUpdateEvent.RefreshRequested, receivedEvent)
    }

    @Test
    fun `malformed json does not crash`() {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val manager = JellyfinWebSocketManager(OkHttpClient()) { }

        manager.handleMessage(webSocket, "{ not valid json }")
    }
}
