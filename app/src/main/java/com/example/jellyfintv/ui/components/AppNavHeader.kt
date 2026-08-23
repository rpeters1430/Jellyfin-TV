package com.example.jellyfintv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.ui.theme.*

fun getLibraryIcon(collectionType: String?, name: String): ImageVector {
    val type = collectionType?.lowercase() ?: ""
    val lowerName = name.lowercase()
    return when {
        type == "movies" || lowerName.contains("movie") || lowerName.contains("film") -> Icons.Default.Movie
        type == "tvshows" || lowerName.contains("tv") || lowerName.contains("series") || lowerName.contains("show") || lowerName.contains("anime") -> Icons.Default.Tv
        type == "playlists" || type == "playlist" || lowerName.contains("playlist") || lowerName.contains("youtube") -> Icons.AutoMirrored.Filled.PlaylistPlay
        type == "music" || lowerName.contains("music") || lowerName.contains("song") || lowerName.contains("audio") -> Icons.Default.MusicNote
        type == "homevideos" || type == "photos" || lowerName.contains("video") || lowerName.contains("photo") || lowerName.contains("stuff") -> Icons.Default.PhotoLibrary
        type == "boxsets" || lowerName.contains("collection") -> Icons.Default.VideoLibrary
        type == "audiobooks" || lowerName.contains("book") -> Icons.AutoMirrored.Filled.MenuBook
        else -> Icons.Default.Folder
    }
}

@Composable
fun AppNavHeader(
    currentTabId: String = "home", // "home", "search", or library parentId
    libraries: List<MediaItem> = emptyList(),
    isDemoMode: Boolean = false,
    username: String = "",
    onNavigateHome: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateLibrary: (MediaItem) -> Unit,
    onOpenThemeSelector: () -> Unit = {},
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Logo + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = "Logo",
                tint = JellyfinBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "JELLYFIN",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.5.sp
                )
            )
            if (isDemoMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JellyfinPurple.copy(alpha = 0.3f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DEMO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = JellyfinPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        // Center: Dynamic Navigation Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            NavTabButton(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = currentTabId == "home",
                onClick = onNavigateHome
            )

            // Search Tab
            NavTabButton(
                title = "Search",
                icon = Icons.Default.Search,
                isSelected = currentTabId == "search",
                onClick = onNavigateSearch
            )

            // Dynamic User Libraries (Movies, TV Shows, Music, Stuff, etc.)
            libraries.forEach { lib ->
                NavTabButton(
                    title = lib.name,
                    icon = getLibraryIcon(lib.collectionType, lib.name),
                    isSelected = currentTabId == lib.id,
                    onClick = { onNavigateLibrary(lib) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right Actions: Theme Selector & User profile / Disconnect
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Selector Button
            val themeSource = remember { MutableInteractionSource() }
            var themeFocused by remember { mutableStateOf(false) }
            val themeHovered by themeSource.collectIsHoveredAsState()
            val themeHighlighted = themeFocused || themeHovered

            IconButton(
                onClick = onOpenThemeSelector,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (themeHighlighted) FocusRingColor else CardSurface)
                    .hoverable(themeSource)
                    .onFocusChanged { themeFocused = it.isFocused }
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Theme & Colors",
                    tint = if (themeHighlighted) Color.White else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Logout Button
            val logoutSource = remember { MutableInteractionSource() }
            var logoutFocused by remember { mutableStateOf(false) }
            val logoutHovered by logoutSource.collectIsHoveredAsState()
            val logoutHighlighted = logoutFocused || logoutHovered

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .hoverable(logoutSource)
                    .onFocusChanged { logoutFocused = it.isFocused },
                colors = ButtonDefaults.colors(
                    containerColor = if (logoutHighlighted) FocusRingColor else CardSurface,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (username.isNotBlank()) username else "Disconnect",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                )
            }
        }
    }
}

@Composable
private fun NavTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighlighted) FocusRingColor
                else if (isSelected) CardSurfaceVariant
                else Color.Transparent
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected || isHighlighted) Color.White else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isSelected || isHighlighted) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        )
    }
}

