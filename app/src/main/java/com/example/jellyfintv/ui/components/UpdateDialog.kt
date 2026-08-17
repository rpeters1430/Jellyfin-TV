package com.example.jellyfintv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.example.jellyfintv.ui.screens.UpdateViewModel
import com.example.jellyfintv.ui.theme.CardSurface
import com.example.jellyfintv.ui.theme.CardSurfaceVariant
import com.example.jellyfintv.ui.theme.FocusRingColor
import com.example.jellyfintv.ui.theme.JellyfinBlue
import com.example.jellyfintv.ui.theme.TextPrimary
import com.example.jellyfintv.ui.theme.TextSecondary

@Composable
fun UpdateDialog(
    state: UpdateViewModel.UiState,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onOpenInstallSettings: () -> Unit
) {
    val info = state.updateInfo
    if (info == null || state.dismissed) return

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.needsInstallPermission, state.errorMessage, state.isDownloading) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(1.dp, CardSurfaceVariant, RoundedCornerShape(16.dp))
                .padding(28.dp)
        ) {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Version ${info.versionName}",
                style = MaterialTheme.typography.titleMedium.copy(color = JellyfinBlue, fontWeight = FontWeight.Bold)
            )

            if (!info.releaseNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = info.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                state.needsInstallPermission -> {
                    Text(
                        text = "Allow this app to install updates in Settings, then try again.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        var focused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onOpenInstallSettings,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { focused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (focused) FocusRingColor else JellyfinBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Open Settings")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onLater) {
                            Text("Later", color = TextPrimary)
                        }
                    }
                }

                state.isDownloading -> {
                    Text(
                        text = "Downloading update... ${(state.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(state.downloadProgress.coerceIn(0f, 1f))
                                .background(JellyfinBlue)
                        )
                    }
                }

                state.errorMessage != null -> {
                    Text(
                        text = "Update failed: ${state.errorMessage}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFF8A8A))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        var focused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onUpdateNow,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { focused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (focused) FocusRingColor else JellyfinBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onLater) {
                            Text("Later", color = TextPrimary)
                        }
                    }
                }

                else -> {
                    Row {
                        var focused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onUpdateNow,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { focused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (focused) FocusRingColor else JellyfinBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Update Now")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = onLater) {
                            Text("Later", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
