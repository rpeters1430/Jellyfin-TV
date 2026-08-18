package com.example.jellyfintv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.tv.material3.*
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.ui.theme.*

@Composable
fun HeroBanner(
    item: MediaItem?,
    backdropUrl: String?,
    onPlayClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    backdropHeaders: Map<String, String> = emptyMap()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // Crossfade animation between focused media backdrops
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 600),
            label = "backdropCrossfade"
        ) { url ->
            if (url != null) {
                AuthenticatedAsyncImage(
                    url = url,
                    headers = backdropHeaders,
                    contentDescription = item?.name ?: "Hero Backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(JellyfinPurple.copy(alpha = 0.4f), DeepBackground)
                            )
                        )
                )
            }
        }

        // Gradients overlay (Left dark fade + bottom dark fade)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepBackground,
                            DeepBackground.copy(alpha = 0.95f),
                            DeepBackground.copy(alpha = 0.6f),
                            Color.Transparent
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

        // Hero Info Content
        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 36.dp, top = 20.dp, end = 36.dp)
                    .widthIn(max = 580.dp)
            ) {
                // Genres / Type tag
                Text(
                    text = (item.genres?.joinToString(" • ") ?: item.type).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = JellyfinBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Chips (Rating, Year, Duration, Official Rating)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (item.communityRating != null && item.communityRating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = RatingStarColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.communityRating),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (item.productionYear != null) {
                        Text(
                            text = "${item.productionYear}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }

                    if (item.durationMinutes > 0) {
                        Text(
                            text = "${item.durationMinutes}m",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }

                    if (!item.officialRating.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, TextSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.officialRating,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Overview
                if (!item.overview.isNullOrBlank()) {
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val playInteractionSource = remember { MutableInteractionSource() }
                    var playFocused by remember { mutableStateOf(false) }
                    val playHovered by playInteractionSource.collectIsHoveredAsState()
                    val playHighlighted = playFocused || playHovered

                    Box(
                        modifier = Modifier
                            .hoverable(interactionSource = playInteractionSource)
                            .onFocusChanged { playFocused = it.isFocused }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (playHighlighted) FocusRingColor else JellyfinBlue)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = null
                            ) {
                                onPlayClick(item)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.playedPercentage > 0f) "Resume" else "Play",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    val detailsInteractionSource = remember { MutableInteractionSource() }
                    var detailsFocused by remember { mutableStateOf(false) }
                    val detailsHovered by detailsInteractionSource.collectIsHoveredAsState()
                    val detailsHighlighted = detailsFocused || detailsHovered

                    Box(
                        modifier = Modifier
                            .hoverable(interactionSource = detailsInteractionSource)
                            .onFocusChanged { detailsFocused = it.isFocused }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (detailsHighlighted) CardSurfaceVariant else CardSurface)
                            .border(
                                width = if (detailsHighlighted) 2.dp else 1.dp,
                                color = if (detailsHighlighted) FocusRingColor else TextSecondary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = detailsInteractionSource,
                                indication = null
                            ) {
                                onDetailsClick(item)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = if (detailsHighlighted) Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (detailsHighlighted) Color.White else TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
