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

data class PersonInfo(
    @SerializedName("Name") val name: String,
    @SerializedName("Id") val id: String? = null,
    @SerializedName("Role") val role: String? = null,
    @SerializedName("Type") val type: String? = null, // "Actor", "Director", "Writer", "Producer", "Composer", etc.
    @SerializedName("PrimaryImageTag") val primaryImageTag: String? = null
)

data class StudioInfo(
    @SerializedName("Name") val name: String,
    @SerializedName("Id") val id: String? = null
)

data class ChapterInfo(
    @SerializedName("StartPositionTicks") val startPositionTicks: Long = 0L,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("ImagePath") val imagePath: String? = null,
    @SerializedName("ImageTag") val imageTag: String? = null
)

data class TrailerInfo(
    @SerializedName("Url") val url: String? = null,
    @SerializedName("Name") val name: String? = null
)

data class MediaItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("OriginalTitle") val originalTitle: String? = null,
    @SerializedName("Overview") val overview: String? = null,
    @SerializedName("Taglines") val taglines: List<String>? = emptyList(),
    @SerializedName("Type") val type: String = "Movie", // Movie, Series, Episode, CollectionFolder, etc.
    @SerializedName("MediaType") val mediaType: String? = null, // Video, Audio, etc.
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("ProductionYear") val productionYear: Int? = null,
    @SerializedName("OfficialRating") val officialRating: String? = null,
    @SerializedName("CommunityRating") val communityRating: Float? = null,
    @SerializedName("CriticRating") val criticRating: Float? = null,
    @SerializedName("PremiereDate") val premiereDate: String? = null,
    @SerializedName("EndDate") val endDate: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Genres") val genres: List<String>? = emptyList(),
    @SerializedName("People") val people: List<PersonInfo>? = emptyList(),
    @SerializedName("Studios") val studios: List<StudioInfo>? = emptyList(),
    @SerializedName("Chapters") val chapters: List<ChapterInfo>? = emptyList(),
    @SerializedName("RemoteTrailers") val remoteTrailers: List<TrailerInfo>? = emptyList(),
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeriesId") val seriesId: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("SeasonId") val seasonId: String? = null,
    @SerializedName("IndexNumber") val episodeIndex: Int? = null,
    @SerializedName("ParentIndexNumber") val seasonIndex: Int? = null,
    @SerializedName("ChildCount") val childCount: Int? = null,
    @SerializedName("UserData") val userData: UserData? = null,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("ImageTags") val imageTags: Map<String, String>? = emptyMap(),
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String>? = emptyList(),
    @SerializedName("MediaSources") val mediaSources: List<MediaSource>? = emptyList(),
    @SerializedName("Container") val container: String? = null,
    @SerializedName("Size") val size: Long? = null,
    @SerializedName("Bitrate") val bitrate: Int? = null
) {
    val durationMinutes: Int
        get() = runTimeTicks?.let { (it / 600_000_000L).toInt() } ?: 0

    val playedPercentage: Float
        get() = userData?.playedPercentage ?: 0f

    val tagline: String?
        get() = taglines?.firstOrNull { it.isNotBlank() }

    val directors: List<PersonInfo>
        get() = people?.filter { it.type.equals("Director", ignoreCase = true) } ?: emptyList()

    val writers: List<PersonInfo>
        get() = people?.filter { it.type.equals("Writer", ignoreCase = true) } ?: emptyList()

    val actors: List<PersonInfo>
        get() = people?.filter { it.type.equals("Actor", ignoreCase = true) } ?: emptyList()

    val videoStream: MediaStream?
        get() = mediaSources?.firstOrNull()?.mediaStreams?.firstOrNull { it.type.equals("Video", ignoreCase = true) }

    val audioStreams: List<MediaStream>
        get() = mediaSources?.firstOrNull()?.mediaStreams?.filter { it.type.equals("Audio", ignoreCase = true) } ?: emptyList()

    val subtitleStreams: List<MediaStream>
        get() = mediaSources?.firstOrNull()?.mediaStreams?.filter { it.type.equals("Subtitle", ignoreCase = true) } ?: emptyList()

    val resolutionBadge: String?
        get() {
            val width = videoStream?.width ?: 0
            val height = videoStream?.height ?: 0
            return when {
                width >= 3800 || height >= 2100 -> "4K UHD"
                width >= 1900 || height >= 1000 -> "1080p FHD"
                width >= 1200 || height >= 700 -> "720p HD"
                width > 0 -> "${height}p"
                else -> null
            }
        }

    val videoRangeBadge: String?
        get() {
            val range = videoStream?.videoRange?.uppercase() ?: videoStream?.videoRangeType?.uppercase()
            return when {
                range?.contains("DOVI") == true || range?.contains("DOLBY") == true -> "Dolby Vision"
                range?.contains("HDR10") == true -> "HDR10"
                range?.contains("HDR") == true -> "HDR"
                else -> null
            }
        }

    val audioBadge: String?
        get() {
            val defaultAudio = audioStreams.firstOrNull { it.isDefault } ?: audioStreams.firstOrNull() ?: return null
            val title = defaultAudio.displayTitle?.uppercase() ?: ""
            val codec = defaultAudio.codec?.uppercase() ?: ""
            val channels = defaultAudio.channels ?: 2
            return when {
                title.contains("ATMOS") -> "Dolby Atmos"
                title.contains("TRUEHD") -> "Dolby TrueHD"
                title.contains("DTS-HD") || title.contains("DTS:X") -> "DTS-HD MA"
                codec.contains("EAC3") || codec.contains("EC-3") -> "Dolby Digital Plus"
                codec.contains("AC3") -> "Dolby Digital"
                channels >= 8 -> "7.1 Surround"
                channels >= 6 -> "5.1 Surround"
                channels == 2 -> "Stereo 2.0"
                else -> null
            }
        }

    val videoCodecBadge: String?
        get() {
            val codec = videoStream?.codec?.uppercase() ?: return null
            return when {
                codec.contains("HEVC") || codec.contains("H265") -> "HEVC"
                codec.contains("AV1") -> "AV1"
                codec.contains("H264") || codec.contains("AVC") -> "H.264"
                codec.contains("VP9") -> "VP9"
                else -> codec
            }
        }
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
    @SerializedName("Size") val size: Long? = null,
    @SerializedName("Bitrate") val bitrate: Int? = null,
    @SerializedName("MediaStreams") val mediaStreams: List<MediaStream>? = emptyList()
)

data class MediaStream(
    @SerializedName("Codec") val codec: String? = null,
    @SerializedName("Type") val type: String? = null, // Video, Audio, Subtitle
    @SerializedName("DisplayTitle") val displayTitle: String? = null,
    @SerializedName("IsDefault") val isDefault: Boolean = false,
    @SerializedName("Index") val index: Int = 0,
    @SerializedName("Language") val language: String? = null,
    @SerializedName("Width") val width: Int? = null,
    @SerializedName("Height") val height: Int? = null,
    @SerializedName("AspectRatio") val aspectRatio: String? = null,
    @SerializedName("VideoRange") val videoRange: String? = null,
    @SerializedName("VideoRangeType") val videoRangeType: String? = null,
    @SerializedName("Channels") val channels: Int? = null,
    @SerializedName("ChannelLayout") val channelLayout: String? = null,
    @SerializedName("BitRate") val bitRate: Int? = null,
    @SerializedName("SampleRate") val sampleRate: Int? = null
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
            taglines = listOf("Mankind was born on Earth. It was never meant to die here."),
            overview = "When Earth becomes uninhabitable, a team of explorers undertakes the most important mission in human history: traveling beyond this galaxy to discover whether mankind has a future among the stars.",
            type = "Movie",
            productionYear = 2024,
            officialRating = "PG-13",
            communityRating = 8.9f,
            criticRating = 92f,
            premiereDate = "2024-11-07",
            genres = listOf("Sci-Fi", "Adventure", "Drama"),
            studios = listOf(StudioInfo(name = "Paramount Pictures"), StudioInfo(name = "Syncopy")),
            people = listOf(
                PersonInfo(name = "Christopher Nolan", type = "Director", role = "Director"),
                PersonInfo(name = "Jonathan Nolan", type = "Writer", role = "Screenplay"),
                PersonInfo(name = "Matthew McConaughey", type = "Actor", role = "Cooper"),
                PersonInfo(name = "Anne Hathaway", type = "Actor", role = "Dr. Brand"),
                PersonInfo(name = "Jessica Chastain", type = "Actor", role = "Murph"),
                PersonInfo(name = "Michael Caine", type = "Actor", role = "Professor Brand"),
                PersonInfo(name = "Hans Zimmer", type = "Composer", role = "Original Score")
            ),
            mediaSources = listOf(
                MediaSource(
                    id = "src-1",
                    container = "mkv",
                    size = 18_400_000_000L,
                    bitrate = 24_500_000,
                    mediaStreams = listOf(
                        MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, aspectRatio = "2.39:1", videoRange = "HDR10", displayTitle = "4K HEVC HDR10"),
                        MediaStream(type = "Audio", codec = "EAC3", channels = 8, isDefault = true, language = "eng", displayTitle = "English Dolby Atmos 7.1"),
                        MediaStream(type = "Subtitle", codec = "subrip", language = "eng", displayTitle = "English [SDH]")
                    )
                )
            ),
            runTimeTicks = 10_140_000_000L,
            userData = UserData(playedPercentage = 45f, playbackPositionTicks = 4560000000L)
        ),
        MediaItem(
            id = "mock-2",
            name = "Cyberpunk Neo Metropolis",
            taglines = listOf("In a world of synthetic dreams, the truth is organic."),
            overview = "In a neon-drenched dystopian city, a lone cyber-detective uncovers a conspiracy that spans from the highest corporate towers to the deepest subterranean slums.",
            type = "Movie",
            productionYear = 2025,
            officialRating = "R",
            communityRating = 8.6f,
            criticRating = 88f,
            premiereDate = "2025-06-18",
            genres = listOf("Action", "Sci-Fi", "Thriller"),
            studios = listOf(StudioInfo(name = "CyberArts Pictures"), StudioInfo(name = "Neon Horizon")),
            people = listOf(
                PersonInfo(name = "Denis Villeneuve", type = "Director", role = "Director"),
                PersonInfo(name = "Ryan Gosling", type = "Actor", role = "Officer K"),
                PersonInfo(name = "Ana de Armas", type = "Actor", role = "Joi"),
                PersonInfo(name = "Harrison Ford", type = "Actor", role = "Rick Deckard")
            ),
            mediaSources = listOf(
                MediaSource(
                    id = "src-2",
                    container = "mp4",
                    mediaStreams = listOf(
                        MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 1600, aspectRatio = "2.40:1", videoRange = "DolbyVision", displayTitle = "4K Dolby Vision"),
                        MediaStream(type = "Audio", codec = "AC3", channels = 6, isDefault = true, language = "eng", displayTitle = "English 5.1 Surround")
                    )
                )
            ),
            runTimeTicks = 7_800_000_000L
        ),
        MediaItem(
            id = "mock-3",
            name = "Cosmic Odyssey: Beyond Light",
            taglines = listOf("The universe doesn't whisper its secrets. It sings them."),
            overview = "Deep in the uncharted sectors of deep space, an outer rim survey team encounters an ancient alien artifact capable of altering the fabric of spacetime itself.",
            type = "Movie",
            productionYear = 2026,
            officialRating = "PG-13",
            communityRating = 9.1f,
            criticRating = 95f,
            genres = listOf("Sci-Fi", "Mystery"),
            studios = listOf(StudioInfo(name = "Solaris Media")),
            people = listOf(
                PersonInfo(name = "Alfonso Cuarón", type = "Director", role = "Director"),
                PersonInfo(name = "Sandra Bullock", type = "Actor", role = "Dr. Ryan Stone"),
                PersonInfo(name = "George Clooney", type = "Actor", role = "Matt Kowalski")
            ),
            mediaSources = listOf(
                MediaSource(
                    id = "src-3",
                    container = "mkv",
                    mediaStreams = listOf(
                        MediaStream(type = "Video", codec = "HEVC", width = 1920, height = 1080, videoRange = "SDR", displayTitle = "1080p FHD"),
                        MediaStream(type = "Audio", codec = "AAC", channels = 2, isDefault = true, displayTitle = "Stereo 2.0")
                    )
                )
            ),
            runTimeTicks = 8_700_000_000L
        ),
        MediaItem(
            id = "mock-4",
            name = "The Last Citadel",
            taglines = listOf("Stand or fall together."),
            overview = "Surrounded by endless wasteland, the remnants of humanity defend their final stronghold against an overwhelming mechanical horde.",
            type = "Movie",
            productionYear = 2023,
            officialRating = "R",
            communityRating = 8.2f,
            criticRating = 79f,
            genres = listOf("Action", "Fantasy"),
            studios = listOf(StudioInfo(name = "Ironclad Studios")),
            people = listOf(
                PersonInfo(name = "George Miller", type = "Director", role = "Director"),
                PersonInfo(name = "Tom Hardy", type = "Actor", role = "Max Rockatansky"),
                PersonInfo(name = "Charlize Theron", type = "Actor", role = "Imperator Furiosa")
            ),
            runTimeTicks = 7_200_000_000L
        )
    )

    val sampleSeries = listOf(
        MediaItem(
            id = "mock-series-1",
            name = "Starlight Chronicles",
            taglines = listOf("In the expanse between the stars, humanity's future begins."),
            overview = "An epic space opera series following the crew of the starship Vanguard as they navigate interstellar diplomacy, uncharted anomalies, and mysterious cosmic entities across the outer rim.",
            type = "Series",
            status = "Continuing",
            productionYear = 2025,
            officialRating = "TV-MA",
            communityRating = 9.2f,
            criticRating = 94f,
            premiereDate = "2025-03-12",
            genres = listOf("Sci-Fi", "Drama", "Adventure"),
            studios = listOf(StudioInfo(name = "Nexus Media Studios"), StudioInfo(name = "Galaxy Vision")),
            people = listOf(
                PersonInfo(name = "Jonathan Nolan", type = "Director", role = "Creator / Showrunner"),
                PersonInfo(name = "Pedro Pascal", type = "Actor", role = "Commander James Hayes"),
                PersonInfo(name = "Rebecca Ferguson", type = "Actor", role = "Dr. Elena Vance"),
                PersonInfo(name = "Karl Urban", type = "Actor", role = "Chief Engineer Jax"),
                PersonInfo(name = "Shohreh Aghdashloo", type = "Actor", role = "Ambassador Kora")
            ),
            mediaSources = listOf(
                MediaSource(
                    id = "src-series-1",
                    container = "mkv",
                    mediaStreams = listOf(
                        MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, videoRange = "HDR10", displayTitle = "4K HDR10"),
                        MediaStream(type = "Audio", codec = "EAC3", channels = 8, isDefault = true, language = "eng", displayTitle = "English Dolby Atmos")
                    )
                )
            )
        ),
        MediaItem(
            id = "mock-series-2",
            name = "Quantum Nexus",
            taglines = listOf("One choice. Infinite consequences."),
            overview = "Parallel realities collide when a particle physics breakthrough inadvertently opens tear points between alternate Earths, sparking a covert war across dimensions.",
            type = "Series",
            status = "Continuing",
            productionYear = 2024,
            officialRating = "TV-14",
            communityRating = 8.8f,
            criticRating = 89f,
            premiereDate = "2024-09-18",
            genres = listOf("Sci-Fi", "Thriller", "Mystery"),
            studios = listOf(StudioInfo(name = "ChronoWorks Television")),
            people = listOf(
                PersonInfo(name = "Alex Garland", type = "Director", role = "Creator / Director"),
                PersonInfo(name = "Oscar Isaac", type = "Actor", role = "Dr. Sean Kelly"),
                PersonInfo(name = "Sonoya Mizuno", type = "Actor", role = "Maya Lin")
            ),
            mediaSources = listOf(
                MediaSource(
                    id = "src-series-2",
                    container = "mp4",
                    mediaStreams = listOf(
                        MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, videoRange = "DolbyVision", displayTitle = "4K Dolby Vision"),
                        MediaStream(type = "Audio", codec = "AC3", channels = 6, isDefault = true, language = "eng", displayTitle = "English 5.1 Surround")
                    )
                )
            )
        )
    )

    val sampleSeasons = mapOf(
        "mock-series-1" to listOf(
            MediaItem(
                id = "mock-season-1",
                name = "Season 1",
                type = "Season",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonIndex = 1,
                productionYear = 2025,
                overview = "The maiden voyage of the starship Vanguard uncovers ancient galactic signals and political tensions across uncharted systems.",
                childCount = 4
            ),
            MediaItem(
                id = "mock-season-2",
                name = "Season 2",
                type = "Season",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonIndex = 2,
                productionYear = 2026,
                overview = "Armed with forbidden technology, Commander Hayes and the crew journey into the dark expanse beyond the outer rim.",
                childCount = 3
            )
        ),
        "mock-series-2" to listOf(
            MediaItem(
                id = "mock-season-2-1",
                name = "Season 1",
                type = "Season",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonIndex = 1,
                productionYear = 2024,
                overview = "A laboratory accident triggers an instantaneous temporal rupture in the heart of the research facility.",
                childCount = 2
            ),
            MediaItem(
                id = "mock-season-2-2",
                name = "Season 2",
                type = "Season",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonIndex = 2,
                productionYear = 2025,
                overview = "Multiple timelines collide, threatening total universal collapse unless the original breach can be sealed.",
                childCount = 2
            )
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
                premiereDate = "2025-03-12",
                officialRating = "TV-MA",
                communityRating = 9.1f,
                criticRating = 95f,
                people = listOf(
                    PersonInfo(name = "Jonathan Nolan", type = "Director", role = "Director"),
                    PersonInfo(name = "Lisa Joy", type = "Writer", role = "Writer"),
                    PersonInfo(name = "Pedro Pascal", type = "Actor", role = "Commander James Hayes"),
                    PersonInfo(name = "Rebecca Ferguson", type = "Actor", role = "Dr. Elena Vance")
                ),
                mediaSources = listOf(
                    MediaSource(
                        id = "src-ep-101",
                        container = "mkv",
                        size = 4_200_000_000L,
                        mediaStreams = listOf(
                            MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, videoRange = "HDR10", displayTitle = "4K HDR10"),
                            MediaStream(type = "Audio", codec = "EAC3", channels = 8, isDefault = true, language = "eng", displayTitle = "English Dolby Atmos")
                        )
                    )
                ),
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
                premiereDate = "2025-03-19",
                officialRating = "TV-MA",
                communityRating = 9.3f,
                criticRating = 92f,
                people = listOf(
                    PersonInfo(name = "Jonathan Nolan", type = "Director", role = "Director"),
                    PersonInfo(name = "Pedro Pascal", type = "Actor", role = "Commander James Hayes"),
                    PersonInfo(name = "Karl Urban", type = "Actor", role = "Chief Engineer Jax")
                ),
                mediaSources = listOf(
                    MediaSource(
                        id = "src-ep-102",
                        container = "mkv",
                        size = 3_900_000_000L,
                        mediaStreams = listOf(
                            MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, videoRange = "HDR10", displayTitle = "4K HDR10"),
                            MediaStream(type = "Audio", codec = "EAC3", channels = 8, isDefault = true, language = "eng", displayTitle = "English Dolby Atmos")
                        )
                    )
                ),
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
                premiereDate = "2025-03-26",
                officialRating = "TV-MA",
                communityRating = 9.0f,
                people = listOf(
                    PersonInfo(name = "Pedro Pascal", type = "Actor", role = "Commander James Hayes"),
                    PersonInfo(name = "Rebecca Ferguson", type = "Actor", role = "Dr. Elena Vance")
                ),
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
                premiereDate = "2025-04-02",
                officialRating = "TV-MA",
                communityRating = 9.4f,
                people = listOf(
                    PersonInfo(name = "Jonathan Nolan", type = "Director", role = "Director"),
                    PersonInfo(name = "Shohreh Aghdashloo", type = "Actor", role = "Ambassador Kora")
                ),
                runTimeTicks = 3_300_000_000L, // 55 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            // Season 2 episodes
            MediaItem(
                id = "mock-ep-105",
                name = "Into the Void",
                overview = "Season 2 Premiere: With new dark matter jump drives, the Vanguard crosses the galactic boundary into uncharted space.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-2",
                seasonName = "Season 2",
                seasonIndex = 2,
                episodeIndex = 1,
                premiereDate = "2026-01-14",
                officialRating = "TV-MA",
                communityRating = 9.5f,
                criticRating = 97f,
                people = listOf(
                    PersonInfo(name = "Jonathan Nolan", type = "Director", role = "Director"),
                    PersonInfo(name = "Pedro Pascal", type = "Actor", role = "Commander James Hayes")
                ),
                mediaSources = listOf(
                    MediaSource(
                        id = "src-ep-105",
                        container = "mkv",
                        mediaStreams = listOf(
                            MediaStream(type = "Video", codec = "HEVC", width = 3840, height = 2160, videoRange = "DolbyVision", displayTitle = "4K Dolby Vision"),
                            MediaStream(type = "Audio", codec = "EAC3", channels = 8, isDefault = true, language = "eng", displayTitle = "English Dolby Atmos")
                        )
                    )
                ),
                runTimeTicks = 3_480_000_000L, // 58 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-106",
                name = "Shadows of Orion",
                overview = "The crew discovers an ancient sentient star cluster communicating via high-energy pulsar bursts.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-2",
                seasonName = "Season 2",
                seasonIndex = 2,
                episodeIndex = 2,
                premiereDate = "2026-01-21",
                officialRating = "TV-MA",
                communityRating = 9.2f,
                runTimeTicks = 3_180_000_000L, // 53 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-107",
                name = "The Sovereign Core",
                overview = "A direct confrontation with an artificial intelligence world threatens to trigger a nova event across three systems.",
                type = "Episode",
                seriesId = "mock-series-1",
                seriesName = "Starlight Chronicles",
                seasonId = "mock-season-2",
                seasonName = "Season 2",
                seasonIndex = 2,
                episodeIndex = 3,
                premiereDate = "2026-01-28",
                officialRating = "TV-MA",
                communityRating = 9.6f,
                criticRating = 98f,
                runTimeTicks = 3_600_000_000L, // 60 min
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
                premiereDate = "2024-09-18",
                officialRating = "TV-14",
                communityRating = 8.8f,
                people = listOf(
                    PersonInfo(name = "Alex Garland", type = "Director", role = "Director"),
                    PersonInfo(name = "Oscar Isaac", type = "Actor", role = "Dr. Sean Kelly")
                ),
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
                premiereDate = "2024-09-25",
                officialRating = "TV-14",
                communityRating = 8.9f,
                runTimeTicks = 2_820_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-203",
                name = "Paradox Protocol",
                overview = "Season 2 Premiere: Multiple Earths begin observing identical atmospheric anomalies simultaneously.",
                type = "Episode",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonId = "mock-season-2-2",
                seasonName = "Season 2",
                seasonIndex = 2,
                episodeIndex = 1,
                premiereDate = "2025-04-10",
                officialRating = "TV-14",
                communityRating = 9.1f,
                runTimeTicks = 2_940_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-ep-204",
                name = "Temporal Collapse",
                overview = "The collapse of the anchor bridge threatens to erase three distinct timelines from history.",
                type = "Episode",
                seriesId = "mock-series-2",
                seriesName = "Quantum Nexus",
                seasonId = "mock-season-2-2",
                seasonName = "Season 2",
                seasonIndex = 2,
                episodeIndex = 2,
                premiereDate = "2025-04-17",
                officialRating = "TV-14",
                communityRating = 9.3f,
                runTimeTicks = 3_120_000_000L,
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

    val samplePlaylists = listOf(
        MediaItem(
            id = "mock-playlist-1",
            name = "Lofi Hip Hop - Beats to Relax & Study",
            overview = "24/7 curated chill beats, ambient melodies, and aesthetic visual soundscapes from YouTube creators.",
            type = "Playlist",
            genres = listOf("Lofi", "Chill", "YouTube"),
            productionYear = 2024,
            seriesName = "Lofi Girl"
        ),
        MediaItem(
            id = "mock-playlist-2",
            name = "Kurzgesagt – In a Nutshell Essentials",
            overview = "Videos explaining science, space, physics, and humanity with animated storytelling.",
            type = "Playlist",
            genres = listOf("Science", "Education", "YouTube"),
            productionYear = 2024,
            seriesName = "Kurzgesagt"
        ),
        MediaItem(
            id = "mock-playlist-3",
            name = "Fireship – Code in 100 Seconds",
            overview = "Fast-paced, high-density overviews and deep dives into modern programming languages and frameworks.",
            type = "Playlist",
            genres = listOf("Technology", "Coding", "YouTube"),
            productionYear = 2025,
            seriesName = "Fireship"
        )
    )

    val samplePlaylistItems = mapOf(
        "mock-playlist-1" to listOf(
            MediaItem(
                id = "mock-pl-item-101",
                name = "Late Night Tokyo Rain • Chill Beats",
                overview = "Smooth jazz hop and rain ambiance relaxing mix.",
                type = "Video",
                seriesName = "Lofi Girl",
                seriesId = "mock-playlist-1",
                episodeIndex = 1,
                runTimeTicks = 1_800_000_000L, // 30 min
                userData = UserData(played = true, playedPercentage = 100f)
            ),
            MediaItem(
                id = "mock-pl-item-102",
                name = "Coffee Shop Study Session • Lo-Fi Chillhop",
                overview = "Warm vinyl crackle and acoustic keys for deep focus.",
                type = "Video",
                seriesName = "Lofi Girl",
                seriesId = "mock-playlist-1",
                episodeIndex = 2,
                runTimeTicks = 2_100_000_000L, // 35 min
                userData = UserData(played = false, playedPercentage = 40f, playbackPositionTicks = 840_000_000L)
            ),
            MediaItem(
                id = "mock-pl-item-103",
                name = "Midnight Highway Drive • Synthwave & Chill",
                overview = "Neon lights, synth pads, and mellow basslines for night contemplation.",
                type = "Video",
                seriesName = "Lofi Girl",
                seriesId = "mock-playlist-1",
                episodeIndex = 3,
                runTimeTicks = 1_500_000_000L, // 25 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-pl-item-104",
                name = "Endless Starfield • Ambient Space Dreams",
                overview = "Cosmic drone and melodic electronic soundscapes.",
                type = "Video",
                seriesName = "Lofi Girl",
                seriesId = "mock-playlist-1",
                episodeIndex = 4,
                runTimeTicks = 2_400_000_000L, // 40 min
                userData = UserData(played = false, playedPercentage = 0f)
            )
        ),
        "mock-playlist-2" to listOf(
            MediaItem(
                id = "mock-pl-item-201",
                name = "The Last Human on Earth",
                overview = "What happens if humanity goes extinct and what remains behind?",
                type = "Video",
                seriesName = "Kurzgesagt",
                seriesId = "mock-playlist-2",
                episodeIndex = 1,
                runTimeTicks = 660_000_000L, // 11 min
                userData = UserData(played = true, playedPercentage = 100f)
            ),
            MediaItem(
                id = "mock-pl-item-202",
                name = "What If We Detonated All Nuclear Bombs at Once?",
                overview = "Simulating the planetary aftermath and physics of the ultimate explosion.",
                type = "Video",
                seriesName = "Kurzgesagt",
                seriesId = "mock-playlist-2",
                episodeIndex = 2,
                runTimeTicks = 720_000_000L, // 12 min
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-pl-item-203",
                name = "The Black Hole Bomb and Cosmic Radiation",
                overview = "Harvesting infinite energy from spinning supermassive black holes.",
                type = "Video",
                seriesName = "Kurzgesagt",
                seriesId = "mock-playlist-2",
                episodeIndex = 3,
                runTimeTicks = 780_000_000L, // 13 min
                userData = UserData(played = false, playedPercentage = 0f)
            )
        ),
        "mock-playlist-3" to listOf(
            MediaItem(
                id = "mock-pl-item-301",
                name = "Kotlin in 100 Seconds",
                overview = "The modern JVM language for Android and multiplatform development.",
                type = "Video",
                seriesName = "Fireship",
                seriesId = "mock-playlist-3",
                episodeIndex = 1,
                runTimeTicks = 135_000_000L,
                userData = UserData(played = true, playedPercentage = 100f)
            ),
            MediaItem(
                id = "mock-pl-item-302",
                name = "Rust in 100 Seconds",
                overview = "Memory safety, zero-cost abstractions, and blazing performance explained.",
                type = "Video",
                seriesName = "Fireship",
                seriesId = "mock-playlist-3",
                episodeIndex = 2,
                runTimeTicks = 140_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            ),
            MediaItem(
                id = "mock-pl-item-303",
                name = "Jetpack Compose in 100 Seconds",
                overview = "Declarative reactive UI toolkit for modern Android and TV apps.",
                type = "Video",
                seriesName = "Fireship",
                seriesId = "mock-playlist-3",
                episodeIndex = 3,
                runTimeTicks = 150_000_000L,
                userData = UserData(played = false, playedPercentage = 0f)
            )
        )
    )
}
