package com.example.jellyfintv.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.model.PersonInfo
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.AuthenticatedAsyncImage
import com.example.jellyfintv.ui.components.MediaCard
import com.example.jellyfintv.ui.theme.*

@Composable
fun MediaDetailScreen(
    itemId: String,
    repository: JellyfinRepository,
    onPlay: (MediaItem) -> Unit,
    onNavigateMedia: ((MediaItem) -> Unit)? = null,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MediaDetailViewModel = viewModel(key = itemId) { MediaDetailViewModel(itemId, repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onLogout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = JellyfinBlue
            )
        } else if (state.errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var backFocused by remember { mutableStateOf(false) }
                Text(
                    text = "Couldn't load this title",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.colors(containerColor = JellyfinBlue, contentColor = Color.White)
                    ) {
                        Text("Retry")
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.onFocusChanged { backFocused = it.isFocused }
                    ) {
                        Text("Back", color = TextPrimary)
                    }
                }
            }
        } else if (state.item != null) {
            val media = state.item!!
            val backdropUrl = viewModel.getBackdropUrl(media.id)
            val posterUrl = viewModel.getPosterUrl(media.id)
            val logoUrl = viewModel.getLogoUrl(media.id)
            val imageHeaders = viewModel.getImageHeaders()
            val isSeries = media.type.equals("Series", ignoreCase = true)
            val isEpisode = media.type.equals("Episode", ignoreCase = true)
            val isPlaylist = media.type.equals("Playlist", ignoreCase = true)

            // Backdrop Art
            AuthenticatedAsyncImage(
                url = backdropUrl,
                headers = imageHeaders,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark Gradients Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                DeepBackground,
                                DeepBackground.copy(alpha = 0.96f),
                                DeepBackground.copy(alpha = 0.75f),
                                DeepBackground.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DeepBackground.copy(alpha = 0.7f),
                                DeepBackground
                            )
                        )
                    )
            )

            if (isEpisode) {
                // Dedicated TV Episode Detail Screen Layout
                EpisodeDetailContent(
                    episode = media,
                    parentSeries = state.parentSeries,
                    siblingEpisodes = state.siblingEpisodes,
                    imageHeaders = imageHeaders,
                    isPlayed = state.isPlayed,
                    onPlay = onPlay,
                    onTogglePlayed = { viewModel.togglePlayed() },
                    onNavigateSeries = { state.parentSeries?.let { s -> onNavigateMedia?.invoke(s) } ?: onBack() },
                    onSelectEpisode = { ep -> onNavigateMedia?.invoke(ep) ?: onPlay(ep) },
                    onBack = onBack,
                    viewModel = viewModel
                )
            } else {
                // Movie, TV Show (Series), or Playlist Detail Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                ) {
                    // Back Button
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.onFocusChanged { backFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (backFocused) FocusRingColor else TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Top Hero Row (Poster + Title + Badges + Synopses + Actions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        // Left Poster Card & Quick Tech Tag
                        Column(
                            modifier = Modifier.width(220.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(330.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CardSurface)
                                    .border(1.dp, CardSurfaceVariant, RoundedCornerShape(16.dp))
                            ) {
                                AuthenticatedAsyncImage(
                                    url = posterUrl,
                                    headers = imageHeaders,
                                    contentDescription = media.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Watched Check Badge
                                if (state.isPlayed) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(JellyfinPurple)
                                            .padding(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Watched",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Resume Progress Bar
                                if (media.playedPercentage > 0f && !state.isPlayed) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(media.playedPercentage / 100f)
                                                .background(JellyfinBlue)
                                        )
                                    }
                                }
                            }

                            // Container / Tech summary badge below poster
                            val containerText = media.container?.uppercase() ?: media.mediaSources?.firstOrNull()?.container?.uppercase()
                            if (containerText != null || media.videoCodecBadge != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val techSummary = listOfNotNull(
                                    containerText,
                                    media.videoCodecBadge,
                                    media.audioBadge
                                ).joinToString(" • ")
                                Text(
                                    text = techSummary,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right Details Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 4.dp)
                        ) {
                            // Type / Genre tag
                            val tagText = when {
                                isPlaylist -> {
                                    val playlistGenres = media.genres?.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                                    if (playlistGenres != null) "PLAYLIST • $playlistGenres" else "PLAYLIST"
                                }
                                isSeries -> {
                                    val seriesGenres = media.genres?.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                                    if (seriesGenres != null) "TV SERIES • $seriesGenres" else "TV SERIES"
                                }
                                else -> (media.genres?.joinToString(" • ") ?: media.type)
                            }
                            Text(
                                text = tagText.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = JellyfinBlue,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Logo Image if present, else Text Title
                            if (logoUrl.isNotBlank()) {
                                AuthenticatedAsyncImage(
                                    url = logoUrl,
                                    headers = imageHeaders,
                                    contentDescription = media.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .height(72.dp)
                                        .wrapContentWidth(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Text(
                                    text = media.name,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Tagline in italics if present
                            if (!media.tagline.isNullOrEmpty()) {
                                Text(
                                    text = "\"${media.tagline}\"",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        color = FocusRingColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Rating & Media Badges Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                // Star Rating
                                if (media.communityRating != null && media.communityRating > 0f) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CardSurfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = RatingStarColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = String.format("%.1f", media.communityRating),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                // Critic Rating (Rotten Tomatoes score badge)
                                if (media.criticRating != null && media.criticRating > 0f) {
                                    MediaBadge(
                                        text = "${media.criticRating.toInt()}% CRITIC",
                                        containerColor = Color(0xFFFA320A).copy(alpha = 0.2f),
                                        textColor = Color(0xFFFF523B),
                                        borderColor = Color(0xFFFA320A).copy(alpha = 0.6f)
                                    )
                                }

                                // Series Status / Release Year
                                if (isSeries) {
                                    val statusText = media.status ?: "Series"
                                    MediaBadge(text = statusText.uppercase(), containerColor = JellyfinBlue.copy(alpha = 0.2f), textColor = JellyfinBlue)
                                    if (media.productionYear != null) {
                                        val yearRange = if (media.endDate != null) "${media.productionYear} - ${media.endDate}" else "${media.productionYear}"
                                        MediaBadge(text = yearRange)
                                    }
                                    if (state.seasons.isNotEmpty()) {
                                        val seasonsCount = "${state.seasons.size} ${if (state.seasons.size == 1) "Season" else "Seasons"}"
                                        MediaBadge(text = seasonsCount)
                                    }
                                } else if (media.productionYear != null) {
                                    MediaBadge(text = "${media.productionYear}")
                                }

                                // Formatted Duration
                                if (isPlaylist && state.playlistItems.isNotEmpty()) {
                                    MediaBadge(text = "${state.playlistItems.size} Videos")
                                    val totalPlaylistTicks = state.playlistItems.sumOf { it.runTimeTicks ?: 0L }
                                    val totalPlaylistMins = (totalPlaylistTicks / 600_000_000L).toInt()
                                    if (totalPlaylistMins > 0) {
                                        val hours = totalPlaylistMins / 60
                                        val mins = totalPlaylistMins % 60
                                        val formattedDuration = if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
                                        MediaBadge(text = formattedDuration)
                                    }
                                } else if (!isSeries && !isPlaylist && media.durationMinutes > 0) {
                                    val hours = media.durationMinutes / 60
                                    val mins = media.durationMinutes % 60
                                    val formattedDuration = if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
                                    MediaBadge(text = formattedDuration)
                                } else if (isSeries && state.episodes.isNotEmpty()) {
                                    MediaBadge(text = "${state.episodes.size} Episodes")
                                }

                                // Official Certification Rating
                                if (!media.officialRating.isNullOrEmpty()) {
                                    MediaBadge(
                                        text = media.officialRating,
                                        containerColor = Color.Transparent,
                                        textColor = TextPrimary,
                                        borderColor = TextSecondary
                                    )
                                }

                                // Resolution Badge (4K UHD / 1080p)
                                media.resolutionBadge?.let { res ->
                                    MediaBadge(
                                        text = res,
                                        containerColor = JellyfinBlue.copy(alpha = 0.25f),
                                        textColor = JellyfinBlue,
                                        borderColor = JellyfinBlue
                                    )
                                }

                                // Video Range Badge (HDR10 / Dolby Vision)
                                media.videoRangeBadge?.let { vRange ->
                                    MediaBadge(
                                        text = vRange,
                                        containerColor = JellyfinPurple.copy(alpha = 0.25f),
                                        textColor = JellyfinPurple,
                                        borderColor = JellyfinPurple
                                    )
                                }

                                // Audio Format Badge (Dolby Atmos / 5.1)
                                media.audioBadge?.let { audio ->
                                    MediaBadge(
                                        text = audio,
                                        containerColor = CardSurfaceVariant,
                                        textColor = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Synopsis Overview
                            if (!media.overview.isNullOrEmpty()) {
                                Text(
                                    text = media.overview,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = TextSecondary,
                                        lineHeight = 22.sp
                                    ),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons (Play, Watched Toggle, Favorite Toggle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val playTarget = when {
                                    isSeries -> state.nextEpisodeToPlay ?: media
                                    isPlaylist -> state.nextPlaylistItemToPlay ?: state.playlistItems.firstOrNull() ?: media
                                    else -> media
                                }
                                val playText = when {
                                    isSeries && state.nextEpisodeToPlay != null -> {
                                        val ep = state.nextEpisodeToPlay!!
                                        val isResume = (ep.userData?.playbackPositionTicks ?: 0L) > 0L
                                        val prefix = if (isResume) "Resume" else "Play Next"
                                        "$prefix S${ep.seasonIndex ?: 1}:E${ep.episodeIndex ?: 1} - ${ep.name}"
                                    }
                                    isPlaylist && state.nextPlaylistItemToPlay != null -> {
                                        val item = state.nextPlaylistItemToPlay!!
                                        val isResume = (item.userData?.playbackPositionTicks ?: 0L) > 0L
                                        val prefix = if (isResume) "Resume" else "Play"
                                        val idx = item.episodeIndex ?: (state.playlistItems.indexOf(item) + 1)
                                        "$prefix #$idx - ${item.name}"
                                    }
                                    isPlaylist && state.playlistItems.isNotEmpty() -> {
                                        "Play All (${state.playlistItems.size} Videos)"
                                    }
                                    media.playedPercentage > 0f -> {
                                        "Resume (${media.playedPercentage.toInt()}%)"
                                    }
                                    else -> {
                                        if (isSeries) "Play Series" else "Play Movie"
                                    }
                                }

                                var playFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { onPlay(playTarget) },
                                    modifier = Modifier.onFocusChanged { playFocused = it.isFocused },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (playFocused) FocusRingColor else JellyfinBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = playText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Mark Watched Toggle Button
                                var watchedFocused by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { viewModel.togglePlayed() },
                                    modifier = Modifier.onFocusChanged { watchedFocused = it.isFocused },
                                    border = ButtonDefaults.border(
                                        border = Border(border = BorderStroke(1.dp, if (watchedFocused) FocusRingColor else TextSecondary))
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlayed) Icons.Default.Check else Icons.Default.Visibility,
                                        contentDescription = "Watched status",
                                        tint = if (state.isPlayed) JellyfinBlue else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isPlayed) "Watched" else "Mark Watched",
                                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                                    )
                                }

                                // Favorite Toggle Button
                                var favFocused by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { viewModel.toggleFavorite() },
                                    modifier = Modifier.onFocusChanged { favFocused = it.isFocused },
                                    border = ButtonDefaults.border(
                                        border = Border(border = BorderStroke(1.dp, if (favFocused) FocusRingColor else TextSecondary))
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (state.isFavorite) JellyfinPurple else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isFavorite) "Favorited" else "Favorite",
                                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                                    )
                                }
                            }

                            // Key Crew & Metadata Info Grid
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                if (media.directors.isNotEmpty()) {
                                    Column {
                                        Text(
                                            text = if (isSeries) "CREATORS / DIRECTORS" else "DIRECTOR",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = media.directors.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }

                                if (media.writers.isNotEmpty()) {
                                    Column {
                                        Text(
                                            text = "WRITERS",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = media.writers.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }

                                if (!media.studios.isNullOrEmpty()) {
                                    Column {
                                        Text(
                                            text = if (isSeries) "NETWORK / STUDIO" else "STUDIO",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = media.studios.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cast & Crew Section
                    if (media.actors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = if (isSeries) "Series Cast" else "Cast & Crew",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(end = 36.dp)
                        ) {
                            items(media.actors, key = { it.id ?: it.name }) { person ->
                                CastCard(
                                    person = person,
                                    imageUrl = viewModel.getPersonImageUrl(person.id, person.name),
                                    imageHeaders = imageHeaders
                                )
                            }
                        }
                    }

                    // TV Show Season & Episodes Browser
                    if (isSeries) {
                        Spacer(modifier = Modifier.height(36.dp))

                        // Season Header & Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(end = 36.dp)
                        ) {
                            Text(
                                text = "Seasons & Episodes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        // Season Selector Pills/Cards
                        if (state.seasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 36.dp)
                            ) {
                                items(state.seasons, key = { it.id }) { season ->
                                    val isSelected = season.id == state.selectedSeasonId
                                    val chipInteractionSource = remember { MutableInteractionSource() }
                                    var isFocused by remember { mutableStateOf(false) }
                                    val isHovered by chipInteractionSource.collectIsHoveredAsState()
                                    val isHighlighted = isFocused || isHovered

                                    Box(
                                        modifier = Modifier
                                            .hoverable(interactionSource = chipInteractionSource)
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .clickable(
                                                interactionSource = chipInteractionSource,
                                                indication = null
                                            ) {
                                                viewModel.selectSeason(season.id)
                                            }
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isHighlighted) FocusRingColor
                                                else if (isSelected) JellyfinBlue
                                                else CardSurface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isHighlighted) FocusRingColor else CardSurfaceVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = season.name,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    color = if (isSelected || isHighlighted) Color.White else TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            val seasonSub = listOfNotNull(
                                                season.productionYear?.toString(),
                                                season.childCount?.let { "$it Episodes" }
                                            ).joinToString(" • ")
                                            if (seasonSub.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = seasonSub,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (isSelected || isHighlighted) Color.White.copy(alpha = 0.8f) else TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Season Overview if available
                        val currentSeason = state.seasons.find { it.id == state.selectedSeasonId }
                        if (currentSeason != null && !currentSeason.overview.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentSeason.overview,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    lineHeight = 20.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isLoadingEpisodes) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(24.dp),
                                color = JellyfinBlue
                            )
                        } else if (state.episodes.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                contentPadding = PaddingValues(end = 36.dp)
                            ) {
                                items(state.episodes, key = { it.id }) { episode ->
                                    EpisodeCard(
                                        episode = episode,
                                        thumbnailUrl = viewModel.getPosterUrl(episode.id),
                                        thumbnailHeaders = imageHeaders,
                                        onPlay = { onPlay(episode) },
                                        onOpenDetail = { viewModel.showEpisodeDetail(episode) }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No episodes found for this season.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    // Playlist Items Browser
                    if (isPlaylist) {
                        Spacer(modifier = Modifier.height(36.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = "Playlist",
                                tint = JellyfinBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Playlist Videos (${state.playlistItems.size})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isLoadingPlaylist) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(24.dp),
                                color = JellyfinBlue
                            )
                        } else if (state.playlistItems.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                contentPadding = PaddingValues(end = 36.dp)
                            ) {
                                items(state.playlistItems, key = { it.id }) { playlistItem ->
                                    val itemIndex = playlistItem.episodeIndex ?: (state.playlistItems.indexOf(playlistItem) + 1)
                                    PlaylistItemCard(
                                        item = playlistItem,
                                        index = itemIndex,
                                        thumbnailUrl = viewModel.getPosterUrl(playlistItem.id),
                                        thumbnailHeaders = imageHeaders,
                                        onPlay = { onPlay(playlistItem) }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No items found in this playlist.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    // More Like This / Recommended Section
                    if (state.similarItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(36.dp))

                        Text(
                            text = if (isSeries) "More Shows Like This" else "More Like This",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = PaddingValues(end = 36.dp)
                        ) {
                            items(state.similarItems, key = { it.id }) { similarItem ->
                                MediaCard(
                                    item = similarItem,
                                    posterUrl = viewModel.getPosterUrl(similarItem.id),
                                    posterHeaders = imageHeaders,
                                    onClick = {
                                        onNavigateMedia?.invoke(similarItem) ?: onPlay(similarItem)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Episode Detail Dialog (Modal Overlay)
            state.selectedEpisodeForDetail?.let { selectedEp ->
                EpisodeDetailModal(
                    episode = selectedEp,
                    imageHeaders = imageHeaders,
                    onDismiss = { viewModel.showEpisodeDetail(null) },
                    onPlay = {
                        viewModel.showEpisodeDetail(null)
                        onPlay(selectedEp)
                    },
                    onTogglePlayed = { viewModel.toggleEpisodePlayed(selectedEp) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun MediaBadge(
    text: String,
    containerColor: Color = CardSurfaceVariant,
    textColor: Color = TextSecondary,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
private fun CastCard(
    person: PersonInfo,
    imageUrl: String,
    imageHeaders: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(targetValue = if (isHighlighted) 1.06f else 1.0f, label = "castScale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(130.dp)
            .scale(scale)
            .hoverable(interactionSource)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(interactionSource = interactionSource)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(CardSurface)
                .border(
                    width = if (isHighlighted) 3.dp else 1.5.dp,
                    color = if (isHighlighted) FocusRingColor else CardSurfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AuthenticatedAsyncImage(
                url = imageUrl,
                headers = imageHeaders,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = person.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) FocusRingColor else TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (!person.role.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = person.role,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: MediaItem,
    thumbnailUrl: String,
    thumbnailHeaders: Map<String, String>,
    onPlay: () -> Unit,
    onOpenDetail: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(targetValue = if (isHighlighted) 1.06f else 1.0f, label = "episodeScale")
    val isWatched = episode.userData?.played == true
    val progress = episode.userData?.playedPercentage ?: 0f

    Column(
        modifier = Modifier
            .width(280.dp)
            .zIndex(if (isHighlighted) 10f else 0f)
            .scale(scale)
            .hoverable(interactionSource = interactionSource)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onOpenDetail()
            }
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .border(
                width = if (isHighlighted) 3.dp else 1.dp,
                color = if (isHighlighted) FocusRingColor else CardSurfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        // Thumbnail Box (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            AuthenticatedAsyncImage(
                url = thumbnailUrl,
                headers = thumbnailHeaders,
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Episode Tag Badge (e.g. S1:E2)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "S${episode.seasonIndex ?: 1}:E${episode.episodeIndex ?: 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Watched checkmark
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(JellyfinPurple)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Quick Play overlay button icon on focus
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JellyfinBlue.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Progress bar
            if (!isWatched && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .background(JellyfinBlue)
                    )
                }
            }
        }

        // Details below thumbnail
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) FocusRingColor else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (episode.durationMinutes > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${episode.durationMinutes}m",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (!episode.premiereDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.premiereDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            if (!episode.overview.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EpisodeDetailContent(
    episode: MediaItem,
    parentSeries: MediaItem?,
    siblingEpisodes: List<MediaItem>,
    imageHeaders: Map<String, String>,
    isPlayed: Boolean,
    onPlay: (MediaItem) -> Unit,
    onTogglePlayed: () -> Unit,
    onNavigateSeries: () -> Unit,
    onSelectEpisode: (MediaItem) -> Unit,
    onBack: () -> Unit,
    viewModel: MediaDetailViewModel
) {
    val currentIndex = siblingEpisodes.indexOfFirst { it.id == episode.id }
    val prevEpisode = if (currentIndex > 0) siblingEpisodes[currentIndex - 1] else null
    val nextEpisode = if (currentIndex in 0 until siblingEpisodes.size - 1) siblingEpisodes[currentIndex + 1] else null
    val backdropUrl = viewModel.getBackdropUrl(episode.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 28.dp)
    ) {
        // Back Button + Series Breadcrumb Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var backFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier.onFocusChanged { backFocused = it.isFocused }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (backFocused) FocusRingColor else TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            val seriesTitle = parentSeries?.name ?: episode.seriesName
            if (!seriesTitle.isNullOrEmpty()) {
                val breadcrumbInteraction = remember { MutableInteractionSource() }
                var isBreadcrumbFocused by remember { mutableStateOf(false) }
                val isBreadcrumbHovered by breadcrumbInteraction.collectIsHoveredAsState()
                val isBreadcrumbHigh = isBreadcrumbFocused || isBreadcrumbHovered

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .hoverable(breadcrumbInteraction)
                        .onFocusChanged { isBreadcrumbFocused = it.isFocused }
                        .clickable(interactionSource = breadcrumbInteraction, indication = null) {
                            onNavigateSeries()
                        }
                        .background(if (isBreadcrumbHigh) FocusRingColor else CardSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "← $seriesTitle ${episode.seasonName?.let { "• $it" } ?: ""}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isBreadcrumbHigh) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Episode Main Section: 16:9 large preview on left, details & actions on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // Episode Thumbnail Preview (16:9)
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .height(236.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .border(1.dp, CardSurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                AuthenticatedAsyncImage(
                    url = backdropUrl,
                    headers = imageHeaders,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Episode Number Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "S${episode.seasonIndex ?: 1} : E${episode.episodeIndex ?: 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Watched status badge
                if (isPlayed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(JellyfinPurple)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Watched",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Resume progress bar
                if (episode.playedPercentage > 0f && !isPlayed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(episode.playedPercentage / 100f)
                                .background(JellyfinBlue)
                        )
                    }
                }
            }

            // Episode Info & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "SEASON ${episode.seasonIndex ?: 1} • EPISODE ${episode.episodeIndex ?: 1}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = JellyfinBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (episode.communityRating != null && episode.communityRating > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = RatingStarColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", episode.communityRating),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (episode.criticRating != null && episode.criticRating > 0f) {
                        MediaBadge(
                            text = "${episode.criticRating.toInt()}% CRITIC",
                            containerColor = Color(0xFFFA320A).copy(alpha = 0.2f),
                            textColor = Color(0xFFFF523B),
                            borderColor = Color(0xFFFA320A).copy(alpha = 0.6f)
                        )
                    }

                    if (!episode.premiereDate.isNullOrEmpty()) {
                        MediaBadge(text = episode.premiereDate)
                    }

                    if (episode.durationMinutes > 0) {
                        MediaBadge(text = "${episode.durationMinutes} min")
                    }

                    if (!episode.officialRating.isNullOrEmpty()) {
                        MediaBadge(text = episode.officialRating)
                    }

                    episode.resolutionBadge?.let { res ->
                        MediaBadge(text = res, containerColor = JellyfinBlue.copy(alpha = 0.25f), textColor = JellyfinBlue, borderColor = JellyfinBlue)
                    }

                    episode.videoRangeBadge?.let { vRange ->
                        MediaBadge(text = vRange, containerColor = JellyfinPurple.copy(alpha = 0.25f), textColor = JellyfinPurple, borderColor = JellyfinPurple)
                    }

                    episode.audioBadge?.let { audio ->
                        MediaBadge(text = audio, containerColor = CardSurfaceVariant, textColor = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Overview
                if (!episode.overview.isNullOrEmpty()) {
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextSecondary,
                            lineHeight = 22.sp
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playText = if (episode.playedPercentage > 0f) "Resume (${episode.playedPercentage.toInt()}%)" else "Play Episode"

                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { onPlay(episode) },
                        modifier = Modifier.onFocusChanged { playFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = if (playFocused) FocusRingColor else JellyfinBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    var watchedFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onTogglePlayed,
                        modifier = Modifier.onFocusChanged { watchedFocused = it.isFocused },
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(1.dp, if (watchedFocused) FocusRingColor else TextSecondary))
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlayed) Icons.Default.Check else Icons.Default.Visibility,
                            contentDescription = "Watched",
                            tint = if (isPlayed) JellyfinBlue else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlayed) "Watched" else "Mark Watched",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                        )
                    }

                    if (prevEpisode != null) {
                        var prevFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { onSelectEpisode(prevEpisode) },
                            modifier = Modifier.onFocusChanged { prevFocused = it.isFocused },
                            border = ButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, if (prevFocused) FocusRingColor else TextSecondary))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Episode",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Prev Ep", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                        }
                    }

                    if (nextEpisode != null) {
                        var nextFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { onSelectEpisode(nextEpisode) },
                            modifier = Modifier.onFocusChanged { nextFocused = it.isFocused },
                            border = ButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, if (nextFocused) FocusRingColor else TextSecondary))
                            )
                        ) {
                            Text(text = "Next Ep", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Episode",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Key Crew & Cast for Episode
                if (episode.directors.isNotEmpty() || episode.writers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (episode.directors.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "DIRECTOR",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = episode.directors.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                        if (episode.writers.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "WRITER",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = episode.writers.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // More Episodes in this Season
        if (siblingEpisodes.size > 1) {
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "More in ${episode.seasonName ?: "This Season"}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(end = 36.dp)
            ) {
                items(siblingEpisodes, key = { it.id }) { ep ->
                    EpisodeCard(
                        episode = ep,
                        thumbnailUrl = viewModel.getPosterUrl(ep.id),
                        thumbnailHeaders = imageHeaders,
                        onPlay = { onPlay(ep) },
                        onOpenDetail = { onSelectEpisode(ep) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeDetailModal(
    episode: MediaItem,
    imageHeaders: Map<String, String>,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onTogglePlayed: () -> Unit,
    viewModel: MediaDetailViewModel
) {
    val backdropUrl = viewModel.getBackdropUrl(episode.id)
    val isWatched = episode.userData?.played == true
    val progress = episode.userData?.playedPercentage ?: 0f

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(680.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(DeepBackground)
                .border(2.dp, CardSurfaceVariant, RoundedCornerShape(20.dp))
                .padding(28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEASON ${episode.seasonIndex ?: 1} • EPISODE ${episode.episodeIndex ?: 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = JellyfinBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 16:9 Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                ) {
                    AuthenticatedAsyncImage(
                        url = backdropUrl,
                        headers = imageHeaders,
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(JellyfinPurple)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (progress > 0f && !isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress / 100f)
                                    .background(JellyfinBlue)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (episode.communityRating != null && episode.communityRating > 0f) {
                        MediaBadge(text = "★ ${String.format("%.1f", episode.communityRating)}")
                    }
                    if (episode.durationMinutes > 0) {
                        MediaBadge(text = "${episode.durationMinutes} min")
                    }
                    if (!episode.premiereDate.isNullOrEmpty()) {
                        MediaBadge(text = episode.premiereDate)
                    }
                    episode.resolutionBadge?.let { MediaBadge(text = it, textColor = JellyfinBlue) }
                    episode.audioBadge?.let { MediaBadge(text = it) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!episode.overview.isNullOrEmpty()) {
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 20.sp
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.onFocusChanged { playFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = if (playFocused) FocusRingColor else JellyfinBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (progress > 0f) "Resume (${progress.toInt()}%)" else "Play Episode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    var watchedFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onTogglePlayed,
                        modifier = Modifier.onFocusChanged { watchedFocused = it.isFocused },
                        border = ButtonDefaults.border(
                            border = Border(border = BorderStroke(1.dp, if (watchedFocused) FocusRingColor else TextSecondary))
                        )
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Check else Icons.Default.Visibility,
                            contentDescription = "Watched",
                            tint = if (isWatched) JellyfinBlue else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWatched) "Watched" else "Mark Watched",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemCard(
    item: MediaItem,
    index: Int,
    thumbnailUrl: String,
    thumbnailHeaders: Map<String, String>,
    onPlay: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(targetValue = if (isHighlighted) 1.05f else 1.0f, label = "playlistItemScale")
    val isWatched = item.userData?.played == true
    val progress = item.userData?.playedPercentage ?: 0f

    Column(
        modifier = Modifier
            .width(260.dp)
            .zIndex(if (isHighlighted) 10f else 0f)
            .scale(scale)
            .hoverable(interactionSource = interactionSource)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onPlay()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(
                width = if (isHighlighted) 3.dp else 1.dp,
                color = if (isHighlighted) FocusRingColor else CardSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Thumbnail Box (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            AuthenticatedAsyncImage(
                url = thumbnailUrl,
                headers = thumbnailHeaders,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Index Tag Badge (#1, #2...)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Watched checkmark
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(JellyfinPurple)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Progress bar
            if (!isWatched && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .background(JellyfinBlue)
                    )
                }
            }
        }

        // Details below thumbnail
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) FocusRingColor else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.durationMinutes > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.durationMinutes}m",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            if (!item.seriesName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.seriesName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = JellyfinBlue,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!item.overview.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.overview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


