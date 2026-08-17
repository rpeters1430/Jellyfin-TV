package com.example.jellyfintv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.HeroBanner
import com.example.jellyfintv.ui.components.MediaRow
import com.example.jellyfintv.ui.theme.*

@Composable
fun HomeScreen(
    repository: JellyfinRepository,
    onMediaSelected: (MediaItem) -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: (MediaItem) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        key = "home_${repository.prefs.serverUrl}_${repository.prefs.userId}_${repository.prefs.token}"
    ) { HomeViewModel(repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onLogout()
    }

    val focusedItem = state.focusedItem
        ?: state.resumeItems.firstOrNull()
        ?: state.latestMovies.firstOrNull()
        ?: state.latestSeries.firstOrNull()
        ?: state.librarySections.firstOrNull()?.items?.firstOrNull()

    val focusedBackdrop = focusedItem?.let { viewModel.getBackdropUrl(it.id) }
    val imageHeaders = viewModel.getImageHeaders()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        if (state.isLoading && !state.hasAnyContent) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = JellyfinBlue
            )
            return@Box
        }

        if (state.errorMessage != null && !state.hasAnyContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Couldn't load your library",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(20.dp))
                var retryFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.onFocusChanged { retryFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = if (retryFocused) FocusRingColor else JellyfinBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Retry")
                }
            }
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Partial-failure banner: some rows loaded, others didn't
            if (state.errorMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Some content failed to load: ${state.errorMessage}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF8A8A))
                        )
                        var retryFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { viewModel.retry() },
                            modifier = Modifier.onFocusChanged { retryFocused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (retryFocused) FocusRingColor else CardSurfaceVariant,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Top Nav Header
            item {
                com.example.jellyfintv.ui.components.AppNavHeader(
                    currentTabId = "home",
                    libraries = state.userLibraries,
                    isDemoMode = viewModel.isDemoMode,
                    username = repository.prefs.username,
                    onNavigateHome = {},
                    onNavigateSearch = onNavigateToSearch,
                    onNavigateLibrary = { lib -> onNavigateToLibrary(lib) },
                    onLogout = onLogout
                )
            }

            // Hero Banner Section
            item {
                HeroBanner(
                    item = focusedItem,
                    backdropUrl = focusedBackdrop,
                    onPlayClick = { media -> onPlayMedia(media) },
                    onDetailsClick = { media -> onMediaSelected(media) },
                    backdropHeaders = imageHeaders
                )
            }

            // My Libraries Quick Access Row
            if (state.userLibraries.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "My Libraries",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 4.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 36.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.userLibraries, key = { it.id }) { lib ->
                                var isFocused by remember { mutableStateOf(false) }
                                val icon = com.example.jellyfintv.ui.components.getLibraryIcon(lib.collectionType, lib.name)
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isFocused) JellyfinPurple.copy(alpha = 0.8f) else CardSurface)
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = if (isFocused) FocusRingColor else CardSurfaceVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onNavigateToLibrary(lib) }
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .focusable()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = lib.name,
                                            tint = if (isFocused) Color.White else JellyfinBlue,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = lib.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Continue Watching Row
            if (state.resumeItems.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Continue Watching",
                        items = state.resumeItems,
                        getPosterUrl = { id -> viewModel.getPosterUrl(id) },
                        onItemClick = { media -> onPlayMedia(media) },
                        onItemFocused = { media -> viewModel.onItemFocused(media) },
                        posterHeaders = imageHeaders
                    )
                }
            }

            // Next Up (TV Episodes) Row
            if (state.nextUpItems.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Next Up",
                        items = state.nextUpItems,
                        getPosterUrl = { id -> viewModel.getPosterUrl(id) },
                        onItemClick = { media -> onMediaSelected(media) },
                        onItemFocused = { media -> viewModel.onItemFocused(media) },
                        posterHeaders = imageHeaders
                    )
                }
            }

            // Latest Movies Row
            if (state.latestMovies.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "Latest Movies",
                        items = state.latestMovies,
                        getPosterUrl = { id -> viewModel.getPosterUrl(id) },
                        onItemClick = { media -> onMediaSelected(media) },
                        onItemFocused = { media -> viewModel.onItemFocused(media) },
                        posterHeaders = imageHeaders
                    )
                }
            }

            // Latest TV Series Row
            if (state.latestSeries.isNotEmpty()) {
                item {
                    MediaRow(
                        title = "TV Shows",
                        items = state.latestSeries,
                        getPosterUrl = { id -> viewModel.getPosterUrl(id) },
                        onItemClick = { media -> onMediaSelected(media) },
                        onItemFocused = { media -> viewModel.onItemFocused(media) },
                        posterHeaders = imageHeaders
                    )
                }
            }

            // Dynamic User Libraries (e.g. "Stuff", "Home Videos & Photos", etc.)
            state.librarySections.forEach { section ->
                item {
                    MediaRow(
                        title = section.title,
                        items = section.items,
                        getPosterUrl = { id -> viewModel.getPosterUrl(id) },
                        onItemClick = { media ->
                            if (media.type == "Movie" || media.type == "Episode" || media.type == "Video" || media.type == "Photo") {
                                onPlayMedia(media)
                            } else {
                                onMediaSelected(media)
                            }
                        },
                        onItemFocused = { media -> viewModel.onItemFocused(media) },
                        posterHeaders = imageHeaders
                    )
                }
            }
        }
    }
}
