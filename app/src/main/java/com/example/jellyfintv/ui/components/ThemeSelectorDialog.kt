package com.example.jellyfintv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.*
import com.example.jellyfintv.ui.theme.*

@Composable
fun ThemeSelectorDialog(
    currentTheme: AppThemePreset,
    onSelectTheme: (AppThemePreset) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.5.dp, CardSurfaceVariant, RoundedCornerShape(20.dp))
                .padding(28.dp)
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(JellyfinBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme Palette",
                                tint = JellyfinBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Color & Appearance",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Select your preferred color theme & UI accent",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    val closeSource = remember { MutableInteractionSource() }
                    var closeFocused by remember { mutableStateOf(false) }
                    val closeHovered by closeSource.collectIsHoveredAsState()
                    val closeHighlighted = closeFocused || closeHovered

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (closeHighlighted) FocusRingColor else CardSurfaceVariant)
                            .hoverable(closeSource)
                            .onFocusChanged { closeFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (closeHighlighted) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Presets Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(AppThemePreset.entries, key = { it.id }) { preset ->
                        val isSelected = preset == currentTheme
                        val interactionSource = remember { MutableInteractionSource() }
                        var isFocused by remember { mutableStateOf(false) }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        val isHighlighted = isFocused || isHovered

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isHighlighted) 5f else 0f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) CardSurfaceVariant else DeepBackground)
                                .border(
                                    width = if (isHighlighted) 3.dp else if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isHighlighted) preset.focusRing else if (isSelected) preset.primary else CardSurfaceVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .hoverable(interactionSource)
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    onSelectTheme(preset)
                                }
                                .onFocusChanged { isFocused = it.isFocused }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header: Title + Active Checkmark
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighlighted) preset.focusRing else TextPrimary
                                        )
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(preset.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    ),
                                    minLines = 2,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Gradient swatch strip
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(preset.gradientStart, preset.gradientEnd)
                                            )
                                        )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Palette circles
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ColorSwatchCircle(color = preset.primary, label = "Primary")
                                    ColorSwatchCircle(color = preset.secondary, label = "Secondary")
                                    ColorSwatchCircle(color = preset.focusRing, label = "Focus")
                                    ColorSwatchCircle(color = preset.deepBackground, label = "Background", hasBorder = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    label: String,
    hasBorder: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (hasBorder) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                else Modifier
            )
    )
}
