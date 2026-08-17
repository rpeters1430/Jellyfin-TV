package com.example.jellyfintv.data.model

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val pw: String,
    @SerializedName("Password") val password: String = pw
)

data class AuthResult(
    @SerializedName("AccessToken") val accessToken: String?,
    @SerializedName("User") val user: UserInfo?,
    @SerializedName("ServerId") val serverId: String?
)

data class UserInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String
)

data class ViewsResponse(
    @SerializedName("Items") val items: List<MediaItem> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0
)

data class ItemsResponse(
    @SerializedName("Items") val items: List<MediaItem> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0
)

data class MediaItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("OriginalTitle") val originalTitle: String? = null,
    @SerializedName("Overview") val overview: String? = null,
    @SerializedName("Type") val type: String = "Movie", // Movie, Series, Episode, CollectionFolder, etc.
    @SerializedName("MediaType") val mediaType: String? = null, // Video, Audio, etc.
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("ProductionYear") val productionYear: Int? = null,
    @SerializedName("OfficialRating") val officialRating: String? = null,
    @SerializedName("CommunityRating") val communityRating: Float? = null,
    @SerializedName("Genres") val genres: List<String>? = emptyList(),
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeriesId") val seriesId: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("SeasonId") val seasonId: String? = null,
    @SerializedName("IndexNumber") val episodeIndex: Int? = null,
    @SerializedName("ParentIndexNumber") val seasonIndex: Int? = null,
    @SerializedName("UserData") val userData: UserData? = null,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("ImageTags") val imageTags: Map<String, String>? = emptyMap(),
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String>? = emptyList(),
    @SerializedName("MediaSources") val mediaSources: List<MediaSource>? = emptyList()
) {
    val durationMinutes: Int
        get() = runTimeTicks?.let { (it / 600_000_000L).toInt() } ?: 0

    val playedPercentage: Float
        get() = userData?.playedPercentage ?: 0f
}

data class UserData(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerializedName("PlayCount") val playCount: Int = 0,
    @SerializedName("IsFavorite") val isFavorite: Boolean = false,
    @SerializedName("Played") val played: Boolean = false,
    @SerializedName("PlayedPercentage") val playedPercentage: Float = 0f
)

data class MediaSource(
    @SerializedName("Id") val id: String,
    @SerializedName("Path") val path: String? = null,
    @SerializedName("Container") val container: String? = null,
    @SerializedName("MediaStreams") val mediaStreams: List<MediaStream>? = emptyList()
)

data class MediaStream(
    @SerializedName("Codec") val codec: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("DisplayTitle") val displayTitle: String? = null,
    @SerializedName("IsDefault") val isDefault: Boolean = false,
    @SerializedName("Index") val index: Int = 0
)

data class PlaystateRequest(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("PositionTicks") val positionTicks: Long = 0L,
    @SerializedName("IsPaused") val isPaused: Boolean = false,
    @SerializedName("MediaSourceId") val mediaSourceId: String? = null
)

// Helper to produce rich mock data for testing/demo mode
object SampleMediaData {
    val sampleMovies = listOf(
        MediaItem(
            id = "mock-1",
            name = "Interstellar Horizon",
            overview = "When Earth becomes uninhabitable, a team of explorers undertakes the most important mission in human history: traveling beyond this galaxy to discover whether mankind has a future among the stars.",
            type = "Movie",
            productionYear = 2024,
            officialRating = "PG-13",
            communityRating = 8.9f,
            genres = listOf("Sci-Fi", "Adventure", "Drama"),
            runTimeTicks = 10_140_000_000L,
            userData = UserData(playedPercentage = 45f, playbackPositionTicks = 4560000000L)
        ),
        MediaItem(
            id = "mock-2",
            name = "Cyberpunk Neo Metropolis",
            overview = "In a neon-drenched dystopian city, a lone cyber-detective uncovers a conspiracy that spans from the highest corporate towers to the deepest subterranean slums.",
            type = "Movie",
            productionYear = 2025,
            officialRating = "R",
            communityRating = 8.6f,
            genres = listOf("Action", "Sci-Fi", "Thriller"),
            runTimeTicks = 7_800_000_000L
        ),
        MediaItem(
            id = "mock-3",
            name = "Cosmic Odyssey: Beyond Light",
            overview = "Deep in the uncharted sectors of deep space, an outer rim survey team encounters an ancient alien artifact capable of altering the fabric of spacetime itself.",
            type = "Movie",
            productionYear = 2026,
            officialRating = "PG-13",
            communityRating = 9.1f,
            genres = listOf("Sci-Fi", "Mystery"),
            runTimeTicks = 8_700_000_000L
        ),
        MediaItem(
            id = "mock-4",
            name = "The Last Citadel",
            overview = "Surrounded by endless wasteland, the remnants of humanity defend their final stronghold against an overwhelming mechanical horde.",
            type = "Movie",
            productionYear = 2023,
            officialRating = "R",
            communityRating = 8.2f,
            genres = listOf("Action", "Fantasy"),
            runTimeTicks = 7_200_000_000L
        )
    )

    val sampleSeries = listOf(
        MediaItem(
            id = "mock-series-1",
            name = "Starlight Chronicles",
            overview = "An epic space opera series following the crew of the starship Vanguard as they navigate interstellar diplomacy, uncharted anomalies, and mysterious cosmic entities.",
            type = "Series",
            productionYear = 2025,
            officialRating = "TV-MA",
            communityRating = 9.0f,
            genres = listOf("Sci-Fi", "Drama", "Adventure")
        ),
        MediaItem(
            id = "mock-series-2",
            name = "Quantum Nexus",
            overview = "Parallel realities collide when a particle physics breakthrough inadvertently opens tear points between alternate Earths.",
            type = "Series",
            productionYear = 2024,
            officialRating = "TV-14",
            communityRating = 8.7f,
            genres = listOf("Sci-Fi", "Thriller")
        )
    )

    val sampleEpisodes = mapOf(
        "mock-series-1" to listOf(
            MediaItem(
                id = "mock-ep-101",
                name = "The Frontier Signal",
                overview = "The Vanguard picks up a mysterious repeating signal originating from an uncharted Dyson swarm candidate in sector 7.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 1,
                runTimeTicks = 3_120_000_000L, // 52 min
                userData = UserData(played = true, playedPercentage = 100f)
            ),
            MediaItem(
                id = "mock-ep-102",
                name = "Singularity's Edge",
                overview = "Navigating through a dense asteroid field orbiting a micro-singularity pushes the ship's warp drive to its limits.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 2,
                runTimeTicks = 2_880_000_000L, // 48 min
                userData = UserData(played = false, playedPercentage = 30f, playbackPositionTicks = 864_000_000L)
            ),
            MediaItem(
                id = "mock-ep-103",
                name = "Echoes of the Ancients",
                overview = "An away team boards a derelict alien structure, awakening an autonomous defense matrix dormant for millennia.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 3,
                runTimeTicks = 3_000_000_000L, // 50 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-104",
                name = "Event Horizon Protocol",
                overview = "Tensions rise among the crew as conflicting diplomatic orders threaten to strand the Vanguard behind enemy lines.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 4,
                runTimeTicks = 3_300_000_000L, // 55 min
                userData = UserData(played = false, playedPercentage = 0f)
            )
        ),
        "mock-series-2" to listOf(
            MediaItem(
                id = "mock-ep-201",
                name = "Split Timeline",
                overview = "A laboratory accident triggers an instantaneous temporal rupture in the heart of the research facility.",
                type = "Episode",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonId = "mock-season-2-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 1,
                runTimeTicks = 2_700_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-202",
                name = "Convergence",
                overview = "The team discovers that objects and people from another timeline are stabilizing in their universe.",
                type = "Episode",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonId = "mock-season-2-1",
                seasonName = "Season 1",
                seasonIndex = 1,
                episodeIndex = 2,
                runTimeTicks = 2_820_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            )
        )
    )

    val sampleMusic = listOf(
        MediaItem(
            id = "mock-music-1",
            name = "Cosmic Ambience",
            seriesName = "Solar Waves",
            overview = "Deep ambient synthwave journey across interstellar space.",
            type = "MusicAlbum",
            productionYear = 2024,
            genres = listOf("Ambient", "Electronic")
        ),
        MediaItem(
            id = "mock-music-2",
            name = "Neon Horizon",
            seriesName = "Synth Voyage",
            overview = "Retro-futuristic electronic beats and vibrant synth melodies.",
            type = "MusicAlbum",
            productionYear = 2023,
            genres = listOf("Synthwave", "Retrowave")
        )
    )

    val sampleHomeVideos = listOf(
        MediaItem(
            id = "mock-video-1",
            name = "Summer Vacation 2024",
            overview = "Family holiday trip to the Mediterranean coast and mountain trails.",
            type = "Video",
            productionYear = 2024,
            runTimeTicks = 1_800_000_000L, // 30 min
            genres = listOf("Home Video")
        ),
        MediaItem(
            id = "mock-video-2",
            name = "Drone Flight over Alps",
            overview = "Aerial 4K landscape capture of snow peaks and alpine lakes.",
            type = "Video",
            productionYear = 2024,
            runTimeTicks = 900_000_000L, // 15 min
            genres = listOf("Travel", "Drone")
        )
    )
}
