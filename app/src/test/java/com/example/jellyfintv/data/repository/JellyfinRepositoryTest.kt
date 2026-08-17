package com.example.jellyfintv.data.repository

import com.example.jellyfintv.data.model.AuthResult
import com.example.jellyfintv.data.model.ItemsResponse
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.model.UserInfo
import com.example.jellyfintv.data.preferences.FakeServerPreferences
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JellyfinRepositoryTest {

    private val gson = Gson()
    private lateinit var server: MockWebServer
    private lateinit var prefs: FakeServerPreferences
    private lateinit var repository: JellyfinRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        prefs = FakeServerPreferences()
        repository = JellyfinRepository(prefs)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl(): String = server.url("/").toString().removeSuffix("/")

    // ---- Demo mode ----

    @Test
    fun `demo mode - authenticate succeeds without hitting the network`() = runTest {
        val result = repository.authenticate("demo", "ignored", "ignored")

        assertTrue(result.isSuccess)
        assertEquals("DEMO", prefs.serverUrl)
        assertTrue(repository.isDemoMode)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `demo mode - resume items are only the sample movies with playback progress`() = runTest {
        repository.authenticate("demo", "u", "p")

        val result = repository.getResumeItems()

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.userData?.playedPercentage ?: 0f > 0f })
    }

    @Test
    fun `demo mode - stream url is the sample video and headers are empty`() = runTest {
        repository.authenticate("demo", "u", "p")

        assertEquals(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            repository.getStreamUrl("mock-1")
        )
        assertTrue(repository.getStreamHeaders().isEmpty())
    }

    // ---- authenticate() against a real server ----

    @Test
    fun `authenticate success stores token, userId and username`() = runTest {
        val body = gson.toJson(
            AuthResult(accessToken = "tok-123", user = UserInfo(id = "user-1", name = "Alice"), serverId = "srv-1")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repository.authenticate(baseUrl(), "alice", "hunter2")

        assertTrue(result.isSuccess)
        assertEquals("tok-123", prefs.token)
        assertEquals("user-1", prefs.userId)
        assertEquals("Alice", prefs.username)
        assertTrue(prefs.serverUrl.startsWith("http://"))
    }

    @Test
    fun `authenticate 401 fails with a clear message and does not store a token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = repository.authenticate(baseUrl(), "alice", "wrong-password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("401"))
        assertEquals("", prefs.token)
    }

    @Test
    fun `authenticate server error fails`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.authenticate(baseUrl(), "alice", "pw")

        assertTrue(result.isFailure)
    }

    @Test
    fun `authenticate unreachable server fails without crashing`() = runTest {
        val deadUrl = baseUrl()
        server.shutdown()

        val result = repository.authenticate(deadUrl, "alice", "pw")

        assertTrue(result.isFailure)
    }

    // ---- library fetches: success / 401 / generic error ----

    @Test
    fun `getLatestMovies success returns the parsed items`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        val body = gson.toJson(
            ItemsResponse(items = listOf(MediaItem(id = "m1", name = "Movie One"), MediaItem(id = "m2", name = "Movie Two")))
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repository.getLatestMovies()

        assertTrue(result.isSuccess)
        assertEquals(listOf("m1", "m2"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `getLatestMovies 401 maps to UnauthorizedException`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        server.enqueue(MockResponse().setResponseCode(401))

        val result = repository.getLatestMovies()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    @Test
    fun `getLatestMovies server error is a distinct failure from 401`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.getLatestMovies()

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is UnauthorizedException)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("500"))
    }

    @Test
    fun `getItemDetails success returns the item`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        server.enqueue(MockResponse().setResponseCode(200).setBody(gson.toJson(MediaItem(id = "m1", name = "Movie One"))))

        val result = repository.getItemDetails("m1")

        assertTrue(result.isSuccess)
        assertEquals("m1", result.getOrThrow().id)
    }

    @Test
    fun `getItemDetails 401 maps to UnauthorizedException`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        server.enqueue(MockResponse().setResponseCode(401))

        val result = repository.getItemDetails("m1")

        assertTrue(result.exceptionOrNull() is UnauthorizedException)
    }

    // ---- playback reporting: regression coverage for the ItemId-was-never-sent bug ----

    @Test
    fun `reportPlayingProgress sends ItemId, PositionTicks and IsPaused in the request body`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        server.enqueue(MockResponse().setResponseCode(200))

        repository.reportPlayingProgress(itemId = "abc-123", positionTicks = 5_000_000L, isPaused = true)

        val request = server.takeRequest()
        val bodyJson = request.body.readUtf8()
        assertTrue("expected ItemId in body: $bodyJson", bodyJson.contains("\"ItemId\":\"abc-123\""))
        assertTrue(bodyJson.contains("\"PositionTicks\":5000000"))
        assertTrue(bodyJson.contains("\"IsPaused\":true"))
    }

    @Test
    fun `reportPlayingStopped sends ItemId and PositionTicks in the request body`() = runTest {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        server.enqueue(MockResponse().setResponseCode(200))

        repository.reportPlayingStopped(itemId = "abc-123", positionTicks = 9_000_000L)

        val request = server.takeRequest()
        val bodyJson = request.body.readUtf8()
        assertTrue(bodyJson.contains("\"ItemId\":\"abc-123\""))
        assertTrue(bodyJson.contains("\"PositionTicks\":9000000"))
    }

    @Test
    fun `reportPlayingProgress never throws even if the server is unreachable`() = runTest {
        prefs.serverUrl = baseUrl()
        server.shutdown()

        // Should complete silently - callers (PlayerScreen) never wrap this in try/catch.
        repository.reportPlayingProgress(itemId = "abc-123", positionTicks = 0L, isPaused = false)
    }

    // ---- stream URL / headers: regression coverage for the api_key-in-URL leak ----

    @Test
    fun `getStreamUrl never includes the access token`() {
        prefs.serverUrl = baseUrl()
        prefs.token = "super-secret-token"

        val url = repository.getStreamUrl("item-1")

        assertFalse(url.contains("super-secret-token"))
        assertFalse(url.contains("api_key"))
        assertTrue(url.contains("item-1"))
    }

    @Test
    fun `getStreamHeaders carries the token in the Authorization header`() {
        prefs.serverUrl = baseUrl()
        prefs.token = "super-secret-token"

        val headers = repository.getStreamHeaders()

        assertTrue(headers["Authorization"]?.contains("Token=\"super-secret-token\"") == true)
    }

    @Test
    fun `getPosterUrl never includes the access token`() {
        prefs.serverUrl = baseUrl()
        prefs.token = "super-secret-token"

        val url = repository.getPosterUrl("item-1")

        assertFalse(url.contains("super-secret-token"))
        assertFalse(url.contains("api_key"))
        assertTrue(url.contains("item-1"))
    }

    @Test
    fun `getBackdropUrl never includes the access token`() {
        prefs.serverUrl = baseUrl()
        prefs.token = "super-secret-token"

        val url = repository.getBackdropUrl("item-1")

        assertFalse(url.contains("super-secret-token"))
        assertFalse(url.contains("api_key"))
        assertTrue(url.contains("item-1"))
    }

    @Test
    fun `logout clears preferences`() {
        prefs.serverUrl = baseUrl()
        prefs.token = "tok"
        prefs.userId = "user-1"
        assertTrue(prefs.isLoggedIn)

        repository.logout()

        assertFalse(prefs.isLoggedIn)
        assertEquals("", prefs.token)
    }
}
