package com.example.jellyfintv.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.jellyfintv.data.api.RetrofitClient
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VideoAspectRatio(val label: String, val resizeMode: Int) {
    FIT("Fit (16:9)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom / Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

private const val SEEK_INCREMENT_MS = 10_000L

// Named distinctly from Player.seekBack()/seekForward() (Media3 members with their own
// default increment) so this fixed 10s increment isn't silently shadowed by those.
private fun ExoPlayer.rewind() = seekTo((currentPosition - SEEK_INCREMENT_MS).coerceAtLeast(0))
private fun ExoPlayer.fastForward() = seekTo((currentPosition + SEEK_INCREMENT_MS).coerceAtMost(duration))

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    media: MediaItem,
    repository: JellyfinRepository,
    onBack: () -> Unit,
    onPlayNext: ((MediaItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val streamUrl = remember(media.id) { repository.getStreamUrl(media.id) }

    var selectedAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var nextEpisode by remember { mutableStateOf<MediaItem?>(null) }
    var autoPlayCountdown by remember { mutableStateOf<Int?>(null) }

    val exoPlayer = remember {
        val okHttpClient = RetrofitClient.buildOkHttpClient(repository.prefs.trustSelfSignedCerts)
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(repository.getStreamHeaders())
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableStateOf(exoPlayer.duration.coerceAtLeast(1L)) }
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentTracks by remember { mutableStateOf(exoPlayer.currentTracks) }
    var lastReportedPositionMs by remember { mutableStateOf(-10_000L) }

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val exitReportScope = remember { CoroutineScope(SupervisorJob()) }

    // Load the current media item into the player, and fetch the next episode if this is
    // part of a series. Keyed on media.id so switching to a new episode (e.g. autoplay)
    // actually swaps the player's source instead of leaving the finished stream loaded.
    LaunchedEffect(media.id) {
        nextEpisode = null
        autoPlayCountdown = null
        playerError = null
        lastReportedPositionMs = -10_000L

        exoPlayer.setMediaItem(Media3Item.fromUri(Uri.parse(streamUrl)))
        val startPositionTicks = media.userData?.playbackPositionTicks ?: 0L
        if (startPositionTicks > 0) {
            exoPlayer.seekTo(startPositionTicks / 10_000L) // Ticks to milliseconds
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val seriesId = media.seriesId
        if (!seriesId.isNullOrBlank()) {
            repository.getEpisodes(seriesId).onSuccess { eps ->
                val currIdx = eps.indexOfFirst { it.id == media.id }
                if (currIdx >= 0 && currIdx + 1 < eps.size) {
                    nextEpisode = eps[currIdx + 1]
                }
            }
        }
    }

    fun exitPlayer() {
        val positionTicks = exoPlayer.currentPosition * 10_000L
        coroutineScope.launch {
            repository.reportPlayingStopped(media.id, positionTicks)
            onBack()
        }
    }

    fun playNextEpisodeNow() {
        val next = nextEpisode ?: return
        val positionTicks = exoPlayer.currentPosition * 10_000L
        coroutineScope.launch {
            repository.reportPlayingStopped(media.id, positionTicks)
            if (onPlayNext != null) {
                onPlayNext(next)
            } else {
                onBack()
            }
        }
    }

    // Periodically update progress state & ping Jellyfin server
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && playerError == null) {
                    coroutineScope.launch {
                        repository.reportPlayingProgress(
                            itemId = media.id,
                            positionTicks = exoPlayer.currentPosition * 10_000L,
                            isPaused = true
                        )
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(1L)
                    playerError = null
                }
                if (state == Player.STATE_ENDED && nextEpisode != null) {
                    playNextEpisodeNow()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = error.errorCodeName.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val positionTicks = exoPlayer.currentPosition * 10_000L
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            // coroutineScope (rememberCoroutineScope) is cancelled at the same point
            // onDispose fires, so it can't be used for this fallback report - it needs
            // to outlive composition teardown. Cancel it once the report completes so
            // it doesn't linger as an orphaned scope for the rest of the app session.
            exitReportScope.launch {
                repository.reportPlayingStopped(media.id, positionTicks)
            }.invokeOnCompletion {
                exitReportScope.cancel()
            }
        }
    }

    // Auto-hide controls overlay after 6s (unless track dialog is open)
    LaunchedEffect(showControls, showTrackDialog) {
        if (showControls && !showTrackDialog) {
            delay(6000)
            showControls = false
        }
    }

    // Ping progress back to Jellyfin and check remaining time for next episode prompt
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (playerError != null) continue
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)

            // Auto-play prompt near end (< 20s remaining)
            val remainingMs = duration - currentPosition
            if (nextEpisode != null && remainingMs in 1..20000 && duration > 30000) {
                autoPlayCountdown = (remainingMs / 1000).toInt()
            } else if (remainingMs > 20000) {
                autoPlayCountdown = null
            }

            // Sync with Jellyfin roughly every 10 seconds of actual playback progress
            // (position-delta based, not time-based, so a paused player doesn't re-fire
            // every second while sitting inside the same 10s window).
            if (currentPosition - lastReportedPositionMs >= 10000) {
                lastReportedPositionMs = currentPosition
                repository.reportPlayingProgress(
                    itemId = media.id,
                    positionTicks = currentPosition * 10_000L,
                    isPaused = !isPlaying
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (showTrackDialog) {
                        if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape) {
                            showTrackDialog = false
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    }

                    showControls = true
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            true
                        }
                        Key.DirectionLeft -> {
                            exoPlayer.rewind()
                            true
                        }
                        Key.DirectionRight -> {
                            exoPlayer.fastForward()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            exitPlayer()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // ExoPlayer Native View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = selectedAspectRatio.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = selectedAspectRatio.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Spinner
        if (isBuffering && playerError == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = JellyfinBlue
            )
        }

        // Playback Error Overlay
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBackground.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Playback failed",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerError ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var retryFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                playerError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            modifier = Modifier.onFocusChanged { retryFocused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (retryFocused) FocusRingColor else JellyfinBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry")
                        }
                        var backFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { exitPlayer() },
                            modifier = Modifier.onFocusChanged { backFocused = it.isFocused },
                            border = ButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, if (backFocused) FocusRingColor else TextSecondary))
                            )
                        ) {
                            Text("Back", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Next Episode Auto-Play Prompt (Top Right)
        if (autoPlayCountdown != null && nextEpisode != null && playerError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface.copy(alpha = 0.95f))
                    .border(2.dp, FocusRingColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Next episode in ${autoPlayCountdown}s",
                        style = MaterialTheme.typography.labelSmall.copy(color = JellyfinBlue, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextEpisode!!.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { playNextEpisodeNow() },
                            colors = ButtonDefaults.colors(containerColor = JellyfinBlue, contentColor = Color.White)
                        ) {
                            Text("Play Now")
                        }
                        OutlinedButton(onClick = { autoPlayCountdown = null }) {
                            Text("Cancel", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && playerError == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepBackground.copy(alpha = 0.85f),
                                Color.Transparent,
                                DeepBackground.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 36.dp, vertical = 24.dp)
            ) {
                // Top Title Bar
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { exitPlayer() },
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
                    Column {
                        Text(
                            text = media.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (!media.seriesName.isNullOrEmpty()) {
                            Text(
                                text = "${media.seriesName} • S${media.seasonIndex ?: 1}:E${media.episodeIndex ?: 1}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                // Bottom Transport Controls & Settings
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Center Buttons + Options
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Quick Info / Speed
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            var speedFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (speedFocused) FocusRingColor else CardSurface)
                                    .clickable {
                                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val nextIdx = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                                        currentSpeed = speeds[nextIdx]
                                        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
                                    }
                                    .onFocusChanged { speedFocused = it.isFocused }
                                    .focusable()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Center Playback Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var rewindFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { exoPlayer.rewind() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (rewindFocused) FocusRingColor else CardSurface)
                                    .onFocusChanged { rewindFocused = it.isFocused }
                            ) {
                                Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = TextPrimary)
                            }

                            var playFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (playFocused) FocusRingColor else JellyfinBlue)
                                    .onFocusChanged { playFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            var fwdFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { exoPlayer.fastForward() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (fwdFocused) FocusRingColor else CardSurface)
                                    .onFocusChanged { fwdFocused = it.isFocused }
                            ) {
                                Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s", tint = TextPrimary)
                            }
                        }

                        // Right Options: Audio / Subtitles & Aspect Ratio
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            var aspectFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (aspectFocused) FocusRingColor else CardSurface)
                                    .clickable {
                                        val modes = VideoAspectRatio.entries
                                        val nextIdx = (modes.indexOf(selectedAspectRatio) + 1) % modes.size
                                        selectedAspectRatio = modes[nextIdx]
                                    }
                                    .onFocusChanged { aspectFocused = it.isFocused }
                                    .focusable()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedAspectRatio.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            var tracksFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showTrackDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (tracksFocused) FocusRingColor else CardSurface)
                                    .onFocusChanged { tracksFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Audio and Subtitles",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    // Progress Track Bar
                    val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val totalDurationMs = duration.coerceAtLeast(1L)
                    val progressFraction = (positionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(JellyfinBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(positionMs),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = formatTime(totalDurationMs),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Subtitles & Audio Track Selector Dialog
        if (showTrackDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showTrackDialog = false },
                contentAlignment = Alignment.CenterEnd
            ) {
                TrackSelectorSheet(
                    tracks = currentTracks,
                    onDismiss = { showTrackDialog = false },
                    onSelectTrack = { trackGroup, trackIndex ->
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, trackIndex))
                            .build()
                        showTrackDialog = false
                    },
                    onDisableSubtitles = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                        showTrackDialog = false
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TrackSelectorSheet(
    tracks: Tracks,
    onDismiss: () -> Unit,
    onSelectTrack: (Tracks.Group, Int) -> Unit,
    onDisableSubtitles: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(CardSurface)
            .padding(24.dp)
            .clickable(enabled = false) {}
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio & Subtitles",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subtitles Section
                item {
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = JellyfinBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var offFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (offFocused) FocusRingColor else CardSurfaceVariant)
                            .clickable { onDisableSubtitles() }
                            .onFocusChanged { offFocused = it.isFocused }
                            .focusable()
                            .padding(12.dp)
                    ) {
                        Text("Off / Disabled", color = TextPrimary)
                    }
                }

                val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                items(textGroups) { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        var isFocused by remember { mutableStateOf(false) }
                        val label = format.label ?: format.language ?: "Subtitle Track #${i + 1}"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFocused) FocusRingColor
                                    else if (isSelected) JellyfinBlue.copy(alpha = 0.4f)
                                    else CardSurfaceVariant
                                )
                                .clickable { onSelectTrack(group, i) }
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .padding(12.dp)
                        ) {
                            Text(text = label, color = TextPrimary)
                        }
                    }
                }

                // Audio Tracks Section
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Audio Track",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = JellyfinBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                items(audioGroups) { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        var isFocused by remember { mutableStateOf(false) }
                        val label = format.label ?: format.language ?: "Audio Track #${i + 1}"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFocused) FocusRingColor
                                    else if (isSelected) JellyfinBlue.copy(alpha = 0.4f)
                                    else CardSurfaceVariant
                                )
                                .clickable { onSelectTrack(group, i) }
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .padding(12.dp)
                        ) {
                            Text(text = label, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

