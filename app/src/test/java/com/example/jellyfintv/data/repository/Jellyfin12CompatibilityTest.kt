package com.example.jellyfintv.data.repository

import com.example.jellyfintv.data.model.AuthResult
import com.example.jellyfintv.data.model.ItemsResponse
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.model.UserInfo
import com.example.jellyfintv.data.preferences.FakeServerPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Pins the request shapes the app sends to what Jellyfin server 12.x (and therefore the Kotlin
 * SDK 1.9.x, which is generated from the same OpenAPI document) actually serves. Jellyfin 12
 * removed the per-user "Users/{userId}/Items…" routes, so a request to one of those 404s: these
 * tests exist so that regression can't slip back in unnoticed.
 */
class Jellyfin12CompatibilityTest {

    private val gson = Gson()
    private lateinit var server: MockWebServer
    private lateinit var prefs: FakeServerPreferences
    private lateinit var repository: JellyfinRepository

    // ItemFields enum from the Jellyfin 12.x OpenAPI spec (the only legal values for `fields=`).
    private val validItemFields = setOf(
        "AirTime", "CanDelete", "CanDownload", "ChannelInfo", "Chapters", "Trickplay", "ChildCount",
        "CumulativeRunTimeTicks", "CustomRating", "DateCreated", "DateLastMediaAdded",
        "DisplayPreferencesId", "Etag", "ExternalUrls", "Genres", "ItemCounts", "MediaSourceCount",
        "MediaSources", "OriginalTitle", "Overview", "ParentId", "Path", "People", "PlayAccess",
        "ProductionLocations", "ProviderIds", "PrimaryImageAspectRatio", "RecursiveItemCount",
        "Settings", "SeriesStudio", "SortName", "SpecialEpisodeNumbers", "Studios", "Taglines",
        "Tags", "RemoteTrailers", "MediaStreams", "SeasonUserData", "DateLastRefreshed",
        "DateLastSaved", "RefreshState", "ChannelImage", "EnableMediaSourceDisplay", "Width",
        "Height", "ExtraIds", "LocalTrailerCount", "IsHD", "SpecialFeatureCount"
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        prefs = FakeServerPreferences().apply {
            serverUrl = server.url("/").toString().removeSuffix("/")
            token = "tok"
            userId = "user-1"
        }
        repository = JellyfinRepository(prefs)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // takeRequest() with no timeout blocks forever if the app never made the call.
    private fun nextRequest(): RecordedRequest =
        server.takeRequest(5, TimeUnit.SECONDS) ?: error("the app sent no request within 5s")

    private fun enqueueItems() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(gson.toJson(ItemsResponse(items = listOf(MediaItem(id = "m1", name = "One"))))))
    }

    private fun assertModernRoute(request: RecordedRequest, expectedPath: String) {
        val url = request.requestUrl!!
        assertEquals(expectedPath, url.encodedPath)
        assertFalse("removed Users/{id}/… route used: ${url.encodedPath}", url.encodedPath.startsWith("/Users/"))
        assertEquals("user-1", url.queryParameter("userId"))
    }

    private fun assertOnlyValidFields(request: RecordedRequest) {
        val fields = request.requestUrl!!.queryParameter("fields")?.split(",").orEmpty().filter { it.isNotBlank() }
        val invalid = fields.filterNot { it in validItemFields }
        assertTrue("invalid ItemFields sent: $invalid", invalid.isEmpty())
    }

    @Test
    fun `sign in sends only Username and Pw`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(gson.toJson(AuthResult("tok", UserInfo("user-1", "Alice"), "srv")))
        )

        repository.authenticate(prefs.serverUrl, "alice", "hunter2")

        val request = nextRequest()
        assertEquals("/Users/AuthenticateByName", request.requestUrl!!.encodedPath)
        val body = gson.fromJson(request.body.readUtf8(), JsonObject::class.java)
        assertEquals(setOf("Username", "Pw"), body.keySet())
        // The MediaBrowser Authorization header is the only sign-in mechanism 12 still allows.
        assertTrue(request.getHeader("Authorization").orEmpty().startsWith("MediaBrowser "))
        assertNull(request.getHeader("X-Emby-Authorization"))
    }

    @Test
    fun `user views use the UserViews route`() = runTest {
        enqueueItems()
        repository.getUserViews()
        assertModernRoute(nextRequest(), "/UserViews")
    }

    @Test
    fun `resume items use the UserItems Resume route`() = runTest {
        enqueueItems()
        repository.getResumeItems()
        val request = nextRequest()
        assertModernRoute(request, "/UserItems/Resume")
        assertOnlyValidFields(request)
    }

    @Test
    fun `latest items use the Items Latest route`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        repository.getLatestItemsByParent("lib-1")
        val request = nextRequest()
        assertModernRoute(request, "/Items/Latest")
        assertEquals("lib-1", request.requestUrl!!.queryParameter("parentId"))
        assertOnlyValidFields(request)
    }

    @Test
    fun `item lists use the Items route`() = runTest {
        enqueueItems()
        enqueueItems()
        enqueueItems()
        enqueueItems()

        repository.getLatestMovies()
        repository.getLatestSeries()
        repository.getItemsByParent("lib-1")
        repository.searchMedia("matrix")

        repeat(4) {
            val request = nextRequest()
            assertModernRoute(request, "/Items")
            assertOnlyValidFields(request)
        }
    }

    @Test
    fun `item details use the Items itemId route`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(gson.toJson(MediaItem(id = "m1", name = "One"))))
        val result = repository.getItemDetails("m1")
        assertTrue(result.exceptionOrNull().toString(), result.isSuccess)
        assertModernRoute(nextRequest(), "/Items/m1")
    }

    @Test
    fun `marking played and unplayed use the UserPlayedItems route`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val markResult = repository.togglePlayed("m1", currentlyPlayed = false)
        assertTrue(markResult.exceptionOrNull().toString(), markResult.isSuccess)
        val unmarkResult = repository.togglePlayed("m1", currentlyPlayed = true)
        assertTrue(unmarkResult.exceptionOrNull().toString(), unmarkResult.isSuccess)

        val played = nextRequest()
        assertEquals("POST", played.method)
        assertModernRoute(played, "/UserPlayedItems/m1")
        val unplayed = nextRequest()
        assertEquals("DELETE", unplayed.method)
        assertModernRoute(unplayed, "/UserPlayedItems/m1")
    }

    @Test
    fun `show, playlist, similar and favorite routes are unchanged and send valid fields`() = runTest {
        repeat(3) { enqueueItems() }
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        repeat(2) { enqueueItems() }

        repository.getSeasons("s1")
        repository.getEpisodes("s1", "season-1")
        repository.getSimilarItems("m1")
        repository.toggleFavorite("m1", isFavorite = false)
        repository.toggleFavorite("m1", isFavorite = true)
        repository.getPlaylistItems("p1")

        val paths = List(6) { nextRequest() }
        assertEquals("/Shows/s1/Seasons", paths[0].requestUrl!!.encodedPath)
        assertEquals("/Shows/s1/Episodes", paths[1].requestUrl!!.encodedPath)
        assertEquals("/Items/m1/Similar", paths[2].requestUrl!!.encodedPath)
        assertEquals("/UserFavoriteItems/m1", paths[3].requestUrl!!.encodedPath)
        assertEquals("/UserFavoriteItems/m1", paths[4].requestUrl!!.encodedPath)
        assertEquals("/Playlists/p1/Items", paths[5].requestUrl!!.encodedPath)
        listOf(paths[0], paths[1], paths[2], paths[5]).forEach { assertOnlyValidFields(it) }
    }

    @Test
    fun `playback reporting uses Sessions Playing routes with ItemId in the body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        repository.reportPlayingProgress("m1", 10_000L, isPaused = false)
        repository.reportPlayingStopped("m1", 20_000L)

        val progress = nextRequest()
        assertEquals("/Sessions/Playing/Progress", progress.requestUrl!!.encodedPath)
        assertEquals("m1", gson.fromJson(progress.body.readUtf8(), JsonObject::class.java)["ItemId"].asString)
        assertEquals("/Sessions/Playing/Stopped", nextRequest().requestUrl!!.encodedPath)
    }
}
