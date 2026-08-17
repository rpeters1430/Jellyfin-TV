package com.example.jellyfintv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.AuthenticatedAsyncImage
import com.example.jellyfintv.ui.theme.*

@Composable
fun MediaDetailScreen(
    itemId: String,
    repository: JellyfinRepository,
    onPlay: (MediaItem) -> Unit,
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
                modifier = Modifier.align(Alignment.Center).padding(48.dp),
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
            val imageHeaders = viewModel.getImageHeaders()
            val isSeries = media.type.equals("Series", ignoreCase = true)

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
                                DeepBackground.copy(alpha = 0.95f),
                                DeepBackground.copy(alpha = 0.65f),
                                DeepBackground.copy(alpha = 0.3f)
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
                                DeepBackground.copy(alpha = 0.8f),
                                DeepBackground
                            )
                        )
                    )
            )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Left Poster Card
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
                    }

                    // Right Details Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp)
                    ) {
                        // Type / Genre tag
                        Text(
                            text = (media.genres?.joinToString(" • ") ?: media.type).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = JellyfinBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Title
                        Text(
                            text = media.name,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Rating & Metadata Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (media.communityRating != null && media.communityRating > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = RatingStarColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", media.communityRating),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            if (media.productionYear != null) {
                                Text(
                                    text = "${media.productionYear}",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                                )
                            }

                            if (!isSeries && media.durationMinutes > 0) {
                                Text(
                                    text = "${media.durationMinutes} min",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                                )
                            } else if (isSeries && state.episodes.isNotEmpty()) {
                                Text(
                                    text = "${state.episodes.size} Episodes",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                                )
                            }

                            if (!media.officialRating.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, TextSecondary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = media.officialRating,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Synopsis
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val playTarget = if (isSeries) (state.nextEpisodeToPlay ?: media) else media
                            val playText = if (isSeries && state.nextEpisodeToPlay != null) {
                                val ep = state.nextEpisodeToPlay!!
                                val isResume = (ep.userData?.playbackPositionTicks ?: 0L) > 0L
                                val prefix = if (isResume) "Resume" else "Play"
                                "$prefix S${ep.seasonIndex ?: 1}:E${ep.episodeIndex ?: 1} - ${ep.name}"
                            } else if (media.playedPercentage > 0f) {
                                "Resume (${media.playedPercentage.toInt()}%)"
                            } else {
                                "Play Stream"
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
                    }
                }

                // TV Show Season & Episodes Browser
                if (isSeries) {
                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = "Episodes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    // Season Selector if multiple seasons exist
                    if (state.seasons.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.seasons.forEach { season ->
                                val isSelected = season.id == state.selectedSeasonId
                                var isFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isFocused) FocusRingColor
                                            else if (isSelected) JellyfinBlue
                                            else CardSurface
                                        )
                                        .clickable { viewModel.selectSeason(season.id) }
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .focusable()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = season.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSelected || isFocused) Color.White else TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
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
                                    onPlay = { onPlay(episode) }
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
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: MediaItem,
    thumbnailUrl: String,
    thumbnailHeaders: Map<String, String>,
    onPlay: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val isWatched = episode.userData?.played == true
    val progress = episode.userData?.playedPercentage ?: 0f

    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusRingColor else CardSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onPlay() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
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
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Episode Tag Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "E${episode.episodeIndex ?: 1}",
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
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
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

