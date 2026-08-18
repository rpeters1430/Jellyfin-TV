package com.example.jellyfintv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.AppNavHeader
import com.example.jellyfintv.ui.components.MediaCard
import com.example.jellyfintv.ui.components.TvTextField
import com.example.jellyfintv.ui.theme.*

@Composable
fun SearchScreen(
    repository: JellyfinRepository,
    onMediaSelected: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateLibrary: (MediaItem) -> Unit,
    onOpenThemeSelector: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SearchViewModel = viewModel { SearchViewModel(repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val imageHeaders = viewModel.getImageHeaders()
    val suggestedGenres = remember { listOf("Sci-Fi", "Action", "Drama", "Comedy", "Thriller", "Adventure") }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppNavHeader(
                currentTabId = "search",
                libraries = state.userLibraries,
                isDemoMode = viewModel.isDemoMode,
                username = repository.prefs.username,
                onNavigateHome = onNavigateHome,
                onNavigateSearch = {},
                onNavigateLibrary = onNavigateLibrary,
                onOpenThemeSelector = onOpenThemeSelector,
                onLogout = onLogout
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp, vertical = 24.dp)
            ) {
                // Header Row: Back button + Search input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onBack()
                        },
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

                    TvTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = "Search movies, series, or genres...",
                        leadingIcon = Icons.Default.Search,
                        trailingContent = if (state.query.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        } else null,
                        imeAction = ImeAction.Search,
                        onImeAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilter.entries.forEach { filter ->
                        val isSelected = state.selectedFilter == filter
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFocused) FocusRingColor
                                    else if (isSelected) JellyfinBlue
                                    else CardSurface
                                )
                                .clickable { viewModel.setFilter(filter) }
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected || isFocused) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Search Content Area
                if (state.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = JellyfinBlue)
                    }
                } else if (state.query.isBlank()) {
                    // Empty query - Show suggested discovery tags
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp)
                    ) {
                        Text(
                            text = "Quick Search by Genre",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            suggestedGenres.forEach { genre ->
                                var isFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isFocused) FocusRingColor else CardSurface)
                                        .border(1.dp, CardSurfaceVariant, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onQueryChange(genre) }
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .focusable()
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isFocused) Color.White else TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (state.filteredResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No results found for \"${state.query}\"",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for a different title or keyword",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                } else {
                    // Results Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(state.filteredResults, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                posterUrl = viewModel.getPosterUrl(item.id),
                                posterHeaders = imageHeaders,
                                onClick = { onMediaSelected(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
