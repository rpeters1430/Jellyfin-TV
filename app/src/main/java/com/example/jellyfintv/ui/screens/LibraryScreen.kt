package com.example.jellyfintv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.AppNavHeader
import com.example.jellyfintv.ui.components.MediaCard
import com.example.jellyfintv.ui.theme.*

@Composable
fun LibraryScreen(
    parentId: String? = null,
    collectionType: String? = null,
    mediaType: String? = null,
    title: String,
    repository: JellyfinRepository,
    onMediaSelected: (MediaItem) -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateLibrary: (MediaItem) -> Unit,
    onOpenThemeSelector: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: LibraryViewModel = viewModel(
        key = "lib_${parentId}_${collectionType}_${mediaType}"
    ) { LibraryViewModel(parentId, collectionType, mediaType, repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val imageHeaders = viewModel.getImageHeaders()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppNavHeader(
                currentTabId = parentId ?: "",
                libraries = state.userLibraries,
                isDemoMode = viewModel.isDemoMode,
                username = repository.prefs.username,
                onNavigateHome = onNavigateHome,
                onNavigateSearch = onNavigateSearch,
                onNavigateLibrary = onNavigateLibrary,
                onOpenThemeSelector = onOpenThemeSelector,
                onLogout = onLogout
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp, vertical = 24.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Sort Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LibrarySort.entries.forEach { sort ->
                            val isSelected = state.selectedSort == sort
                            var isFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isFocused) FocusRingColor
                                        else if (isSelected) JellyfinBlue
                                        else CardSurface
                                    )
                                    .clickable { viewModel.setSort(sort) }
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sort.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected || isFocused) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = JellyfinBlue)
                    }
                } else if (state.errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Couldn't load $title",
                                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.colors(containerColor = JellyfinBlue)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in this library",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                posterUrl = viewModel.getPosterUrl(item.id),
                                posterHeaders = imageHeaders,
                                onClick = {
                                    if (item.type == "Movie" || item.type == "Episode" || item.type == "Video" || item.type == "Audio") {
                                        onPlayMedia(item)
                                    } else {
                                        onMediaSelected(item)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
