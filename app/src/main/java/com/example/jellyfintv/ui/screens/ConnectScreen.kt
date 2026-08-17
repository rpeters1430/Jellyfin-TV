package com.example.jellyfintv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.TvTextField
import com.example.jellyfintv.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    repository: JellyfinRepository,
    onConnected: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var serverUrl by remember { mutableStateOf(repository.prefs.serverUrl.ifEmpty { "http://100.x.y.z:8096" }) }
    var username by remember { mutableStateOf(repository.prefs.username.ifEmpty { "" }) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var trustSelfSignedCerts by remember { mutableStateOf(repository.prefs.trustSelfSignedCerts) }

    fun submitLogin() {
        if (isLoading) return
        keyboardController?.hide()
        focusManager.clearFocus()

        if (serverUrl.trim().lowercase() == "demo") {
            repository.prefs.serverUrl = "DEMO"
            repository.prefs.token = "demo-token"
            repository.prefs.userId = "demo-user-id"
            repository.prefs.username = "Demo User"
            onConnected()
            return
        }

        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = repository.authenticate(serverUrl, username, password, trustSelfSignedCerts)
            isLoading = false
            if (result.isSuccess) {
                onConnected()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect to Jellyfin server"
            }
        }
    }

    fun enterDemo() {
        keyboardController?.hide()
        focusManager.clearFocus()
        repository.prefs.serverUrl = "DEMO"
        repository.prefs.token = "demo-token"
        repository.prefs.userId = "demo-user-id"
        repository.prefs.username = "Demo User"
        onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        // Gradient ambient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            JellyfinPurple.copy(alpha = 0.35f),
                            JellyfinBlue.copy(alpha = 0.15f),
                            DeepBackground
                        ),
                        radius = 1200f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Jellyfin TV Logo",
                        tint = JellyfinBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Jellyfin TV",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect to your media server",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = JellyfinBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stream your movies, TV shows, and media directly on your Android TV via Local network, Tailscale IP (100.x.y.z:8096), or domain.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                )
            }

            // Right Form Card
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .border(1.dp, CardSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(32.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Server Login",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    // Server URL Input
                    TvTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = "Tailscale / Server Address & Port",
                        leadingIcon = Icons.Default.Dns,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Username Input
                    TvTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        leadingIcon = Icons.Default.Person,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Password Input
                    TvTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = { submitLogin() }
                    )

                    // Self-signed certificate opt-in
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { trustSelfSignedCerts = !trustSelfSignedCerts }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = trustSelfSignedCerts, onCheckedChange = { trustSelfSignedCerts = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trust self-signed certificate (unsafe)",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B1E22))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFF8A8A),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Connect Button
                    var connectFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onFocusChanged { connectFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (connectFocused) FocusRingColor else JellyfinBlue)
                            .clickable { submitLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLoading) "Connecting to Server..." else "Connect & Sign In",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    // Demo Mode Quick Launch Button
                    var demoFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .onFocusChanged { demoFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurfaceVariant)
                            .border(
                                width = if (demoFocused) 2.dp else 1.dp,
                                color = if (demoFocused) FocusRingColor else CardSurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { enterDemo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Explore in Demo Mode (Sample Server)",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }
    }
}

