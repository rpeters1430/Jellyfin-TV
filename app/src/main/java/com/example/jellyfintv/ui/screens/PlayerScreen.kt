package com.example.jellyfintv.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import com.example.jellyfintv.ui.components.AuthenticatedAsyncImage
import com.example.jellyfintv.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
enum class VideoAspectRatio(val label: String, val resizeMode: Int) {
    FIT("Fit (16:9)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom / Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

enum class ActionHudType {
    SEEK_FORWARD,
    SEEK_REWIND,
    PLAY,
    PAUSE
}

data class ActionHudState(
    val type: ActionHudType,
    val seconds: Int = 10,
    val triggerId: Long = System.currentTimeMillis()
)

private const val SEEK_INCREMENT_MS = 10_000L

private fun ExoPlayer.rewind() = seekTo((currentPosition - SEEK_INCREMENT_MS).coerceAtLeast(0))
private fun ExoPlayer.fastForward() {
    val target = currentPosition + SEEK_INCREMENT_MS
    // duration is C.TIME_UNSET (negative) until the stream is ready; clamping to it would seek to 0.
    seekTo(if (duration == C.TIME_UNSET) target else target.coerceAtMost(duration))
}

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
    val imageHeaders = remember { repository.getStreamHeaders() }

    var selectedAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var showPlaylistDrawer by remember { mutableStateOf(false) }
    var showInfoOverlay by remember { mutableStateOf(false) }
    var nextEpisode by remember { mutableStateOf<MediaItem?>(null) }
    var allSeasonEpisodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var nextPlaylistItem by remember { mutableStateOf<MediaItem?>(null) }
    var previousPlaylistItem by remember { mutableStateOf<MediaItem?>(null) }
    var allPlaylistItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
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
    var bufferedPosition by remember { mutableStateOf(exoPlayer.bufferedPosition) }
    var duration by remember { mutableStateOf(exoPlayer.duration.coerceAtLeast(1L)) }
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentTracks by remember { mutableStateOf(exoPlayer.currentTracks) }
    var lastReportedPositionMs by remember { mutableStateOf(-10_000L) }

    // Transient HUD State
    var actionHud by remember { mutableStateOf<ActionHudState?>(null) }
    var lastSeekDirection by remember { mutableStateOf<ActionHudType?>(null) }
    var lastSeekTime by remember { mutableStateOf(0L) }
    var accumulatedSeekSeconds by remember { mutableStateOf(10) }

    val focusRequester = remember { FocusRequester() }
    val playButtonFocus = remember { FocusRequester() }
    // True only while the root box itself holds focus (not one of the control buttons).
    var rootFocused by remember { mutableStateOf(true) }
    // Bumped on every key press so the auto-hide timer restarts while navigating the controls.
    var controlsInteractionTick by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val exitReportScope = remember { CoroutineScope(SupervisorJob()) }

    // Keep screen on during active playback to prevent the streaming device / Android TV
    // from entering screensaver or ambient/sleep mode.
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper && ctx !is Activity) {
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }

    DisposableEffect(isPlaying) {
        val window = activity?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-dismiss transient action HUD
    LaunchedEffect(actionHud?.triggerId) {
        if (actionHud != null) {
            delay(900)
            actionHud = null
        }
    }

    // Load media & episodes / playlist queue
    LaunchedEffect(media.id) {
        nextEpisode = null
        allSeasonEpisodes = emptyList()
        nextPlaylistItem = null
        previousPlaylistItem = null
        allPlaylistItems = emptyList()
        autoPlayCountdown = null
        playerError = null
        lastReportedPositionMs = -10_000L

        // If a playlist container was passed directly, play its first unwatched item
        if (media.type.equals("Playlist", ignoreCase = true)) {
            val items = repository.getPlaylistItems(media.id).getOrNull().orEmpty()
            if (items.isEmpty() || onPlayNext == null) {
                // There's no stream behind a playlist container itself, so don't try to play it.
                playerError = "Nothing to play"
                return@LaunchedEffect
            }
            onPlayNext(items.firstOrNull { it.userData?.played != true } ?: items.first())
            return@LaunchedEffect
        }

        exoPlayer.setMediaItem(Media3Item.fromUri(Uri.parse(streamUrl)))
        val startPositionTicks = media.userData?.playbackPositionTicks ?: 0L
        if (startPositionTicks > 0) {
            exoPlayer.seekTo(startPositionTicks / 10_000L)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val seriesId = media.seriesId
        if (!seriesId.isNullOrBlank()) {
            if (media.type.equals("Episode", ignoreCase = true)) {
                repository.getEpisodes(seriesId).onSuccess { eps ->
                    allSeasonEpisodes = eps
                    val currIdx = eps.indexOfFirst { it.id == media.id }
                    if (currIdx >= 0 && currIdx + 1 < eps.size) {
                        nextEpisode = eps[currIdx + 1]
                    }
                }
            } else {
                repository.getPlaylistItems(seriesId).onSuccess { items ->
                    if (items.isNotEmpty()) {
                        allPlaylistItems = items
                        val currIdx = items.indexOfFirst { it.id == media.id }
                        if (currIdx >= 0) {
                            if (currIdx + 1 < items.size) {
                                nextPlaylistItem = items[currIdx + 1]
                            }
                            if (currIdx > 0) {
                                previousPlaylistItem = items[currIdx - 1]
                            }
                        }
                    }
                }
            }
        }
    }

    // Fire-and-forget: never make navigation wait on the network (the read timeout is 30s).
    fun reportStopped() {
        val itemId = media.id
        val positionTicks = exoPlayer.currentPosition * 10_000L
        exitReportScope.launch { repository.reportPlayingStopped(itemId, positionTicks) }
    }

    // The final stop report for leaving the player is sent by the DisposableEffect's onDispose,
    // which covers this path and every other one (e.g. MainActivity's system BackHandler).
    fun exitPlayer() {
        onBack()
    }

    fun playMedia(next: MediaItem) {
        reportStopped()
        if (onPlayNext != null) {
            onPlayNext(next)
        } else {
            onBack()
        }
    }

    fun playNextEpisodeNow() {
        nextEpisode?.let { playMedia(it) }
    }

    fun playNextPlaylistItemNow() {
        nextPlaylistItem?.let { playMedia(it) }
    }

    fun playPreviousPlaylistItemNow() {
        previousPlaylistItem?.let { playMedia(it) }
    }

    fun switchEpisode(ep: MediaItem) {
        showEpisodesDrawer = false
        playMedia(ep)
    }

    fun switchPlaylistItem(item: MediaItem) {
        showPlaylistDrawer = false
        playMedia(item)
    }

    // The player listener below is registered once, so it must go through these rather than
    // capture the first composition's `media`/`onPlayNext` (which go stale after auto-play).
    val latestMedia by rememberUpdatedState(media)
    val latestPlayNextEpisode by rememberUpdatedState({ playNextEpisodeNow() })
    val latestPlayNextPlaylistItem by rememberUpdatedState({ playNextPlaylistItemNow() })

    fun triggerRewind() {
        val now = System.currentTimeMillis()
        if (lastSeekDirection == ActionHudType.SEEK_REWIND && now - lastSeekTime < 1200) {
            accumulatedSeekSeconds += 10
        } else {
            accumulatedSeekSeconds = 10
        }
        lastSeekDirection = ActionHudType.SEEK_REWIND
        lastSeekTime = now
        exoPlayer.rewind()
        actionHud = ActionHudState(ActionHudType.SEEK_REWIND, accumulatedSeekSeconds, now)
        showControls = true
    }

    fun triggerFastForward() {
        val now = System.currentTimeMillis()
        if (lastSeekDirection == ActionHudType.SEEK_FORWARD && now - lastSeekTime < 1200) {
            accumulatedSeekSeconds += 10
        } else {
            accumulatedSeekSeconds = 10
        }
        lastSeekDirection = ActionHudType.SEEK_FORWARD
        lastSeekTime = now
        exoPlayer.fastForward()
        actionHud = ActionHudState(ActionHudType.SEEK_FORWARD, accumulatedSeekSeconds, now)
        showControls = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            actionHud = ActionHudState(ActionHudType.PAUSE, 0, System.currentTimeMillis())
        } else {
            exoPlayer.play()
            actionHud = ActionHudState(ActionHudType.PLAY, 0, System.currentTimeMillis())
        }
        showControls = true
    }

    val isSubtitleActive = remember(currentTracks) {
        currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
    }

    fun toggleSubtitles() {
        if (isSubtitleActive) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val firstTextGroup = currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_TEXT }
            if (firstTextGroup != null && firstTextGroup.length > 0) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(firstTextGroup.mediaTrackGroup, 0))
                    .build()
            }
        }
    }

    // Listener for player events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && playerError == null) {
                    coroutineScope.launch {
                        repository.reportPlayingProgress(
                            itemId = latestMedia.id,
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
                    bufferedPosition = exoPlayer.bufferedPosition
                    playerError = null
                }
                if (state == Player.STATE_ENDED) {
                    if (nextEpisode != null) {
                        latestPlayNextEpisode()
                    } else if (nextPlaylistItem != null) {
                        latestPlayNextPlaylistItem()
                    }
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
            val itemId = latestMedia.id
            val positionTicks = exoPlayer.currentPosition * 10_000L
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            exitReportScope.launch {
                repository.reportPlayingStopped(itemId, positionTicks)
            }
        }
    }

    // Auto-hide controls overlay after 6s (unless sheets/dialogs open)
    LaunchedEffect(showControls, showTrackDialog, showEpisodesDrawer, showPlaylistDrawer, showInfoOverlay, controlsInteractionTick) {
        if (showControls && !showTrackDialog && !showEpisodesDrawer && !showPlaylistDrawer && !showInfoOverlay) {
            delay(6000)
            showControls = false
        }
    }

    // The focused element leaves composition when the controls hide or a sheet/drawer closes;
    // take focus back so the remote goes to seek/play-pause instead of into nothing.
    LaunchedEffect(showControls, showTrackDialog, showEpisodesDrawer, showPlaylistDrawer) {
        if (!showTrackDialog && !showEpisodesDrawer && !showPlaylistDrawer) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Periodic position, buffer, and Jellyfin progress update
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (playerError != null) continue
            currentPosition = exoPlayer.currentPosition
            bufferedPosition = exoPlayer.bufferedPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)

            // Auto-play prompt near end (< 20s remaining)
            val remainingMs = duration - currentPosition
            if (nextEpisode != null && remainingMs in 1..20000 && duration > 30000) {
                autoPlayCountdown = (remainingMs / 1000).toInt()
            } else if (remainingMs > 20000) {
                autoPlayCountdown = null
            }

            // Sync with Jellyfin roughly every 10 seconds of playback
            // abs(): after rewinding past the last report the difference goes negative, which
            // would otherwise silence progress reports until playback caught back up.
            if (kotlin.math.abs(currentPosition - lastReportedPositionMs) >= 10000) {
                lastReportedPositionMs = currentPosition
                repository.reportPlayingProgress(
                    itemId = latestMedia.id,
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
            .onFocusChanged { rootFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                controlsInteractionTick++
                val isBack = keyEvent.key == Key.Back || keyEvent.key == Key.Escape

                when {
                    // Sheets/drawers: Back closes them; every other key falls through to the
                    // focus system so the D-pad can move around inside them.
                    showTrackDialog -> isBack.also { if (it) showTrackDialog = false }
                    showEpisodesDrawer -> isBack.also { if (it) showEpisodesDrawer = false }
                    showPlaylistDrawer -> isBack.also { if (it) showPlaylistDrawer = false }
                    isBack -> {
                        when {
                            showInfoOverlay -> showInfoOverlay = false
                            showControls -> showControls = false
                            else -> exitPlayer()
                        }
                        true
                    }
                    // A control button has focus: leave D-pad keys to it and the focus system.
                    // (Consuming them here is what made the control bar unreachable by remote.)
                    !rootFocused -> false
                    else -> when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            togglePlayPause()
                            true
                        }
                        Key.DirectionLeft -> {
                            triggerRewind()
                            true
                        }
                        Key.DirectionRight -> {
                            triggerFastForward()
                            true
                        }
                        Key.DirectionUp -> {
                            showControls = true
                            showInfoOverlay = !showInfoOverlay
                            true
                        }
                        Key.DirectionDown -> {
                            // First press reveals the controls; the next moves focus onto them.
                            if (showControls) {
                                runCatching { playButtonFocus.requestFocus() }
                            } else {
                                showControls = true
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
    ) {
        // ExoPlayer Native View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = selectedAspectRatio.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.keepScreenOn = isPlaying
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

        // Center Transient Action HUD (Rewind/Fast-Forward/Play/Pause pulse)
        AnimatedVisibility(
            visible = actionHud != null,
            enter = fadeIn() + scaleIn(initialScale = 0.82f),
            exit = fadeOut() + scaleOut(targetScale = 1.15f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            actionHud?.let { hud ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DeepBackground.copy(alpha = 0.88f))
                        .border(1.5.dp, FocusRingColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (hud.type) {
                            ActionHudType.SEEK_REWIND -> {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind",
                                    tint = FocusRingColor,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "-${hud.seconds}s",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                            ActionHudType.SEEK_FORWARD -> {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward",
                                    tint = FocusRingColor,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "+${hud.seconds}s",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                            ActionHudType.PLAY -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = JellyfinBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            ActionHudType.PAUSE -> {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Pause",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
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
                        val retrySource = remember { MutableInteractionSource() }
                        var retryFocused by remember { mutableStateOf(false) }
                        val retryHovered by retrySource.collectIsHoveredAsState()
                        val retryHighlighted = retryFocused || retryHovered

                        Button(
                            onClick = {
                                playerError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            modifier = Modifier
                                .hoverable(retrySource)
                                .onFocusChanged { retryFocused = it.isFocused },
                            colors = ButtonDefaults.colors(
                                containerColor = if (retryHighlighted) FocusRingColor else JellyfinBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry")
                        }

                        val backSource = remember { MutableInteractionSource() }
                        var backFocused by remember { mutableStateOf(false) }
                        val backHovered by backSource.collectIsHoveredAsState()
                        val backHighlighted = backFocused || backHovered

                        OutlinedButton(
                            onClick = { exitPlayer() },
                            modifier = Modifier
                                .hoverable(backSource)
                                .onFocusChanged { backFocused = it.isFocused },
                            border = ButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, if (backHighlighted) FocusRingColor else TextSecondary))
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

        // Media Info / Synopsis Overlay (Shown on D-Pad Up or Info toggle)
        AnimatedVisibility(
            visible = showInfoOverlay && playerError == null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 36.dp, top = 88.dp, end = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface.copy(alpha = 0.92f))
                    .border(1.dp, CardSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    Text(
                        text = media.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (!media.overview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = media.overview,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            ),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                DeepBackground.copy(alpha = 0.88f),
                                Color.Transparent,
                                DeepBackground.copy(alpha = 0.96f)
                            )
                        )
                    )
                    .padding(horizontal = 36.dp, vertical = 24.dp)
            ) {
                // Top Title & Technical Spec Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val backSource = remember { MutableInteractionSource() }
                        var backFocused by remember { mutableStateOf(false) }
                        val backHovered by backSource.collectIsHoveredAsState()
                        val backHighlighted = backFocused || backHovered

                        IconButton(
                            onClick = { exitPlayer() },
                            modifier = Modifier
                                .hoverable(backSource)
                                .onFocusChanged { backFocused = it.isFocused }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (backHighlighted) FocusRingColor else TextPrimary,
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
                                val subText = if (allPlaylistItems.isNotEmpty()) {
                                    val currIdx = allPlaylistItems.indexOfFirst { it.id == media.id }
                                    val idxText = if (currIdx >= 0) " • Video ${currIdx + 1} of ${allPlaylistItems.size}" else ""
                                    "${media.seriesName}$idxText"
                                } else {
                                    "${media.seriesName} • S${media.seasonIndex ?: 1}:E${media.episodeIndex ?: 1}"
                                }
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }
                    }

                    // Top Right Quality Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allPlaylistItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(JellyfinBlue.copy(alpha = 0.25f))
                                    .border(1.dp, JellyfinBlue, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PLAYLIST",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = JellyfinBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedAspectRatio.label.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isSubtitleActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(JellyfinBlue.copy(alpha = 0.3f))
                                    .border(1.dp, JellyfinBlue, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CC ON",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = JellyfinBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Bottom Transport Controls & Timeline Scrubber
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Options: Speed & In-Player Episode / Playlist Browser
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val speedSource = remember { MutableInteractionSource() }
                            var speedFocused by remember { mutableStateOf(false) }
                            val speedHovered by speedSource.collectIsHoveredAsState()
                            val speedHighlighted = speedFocused || speedHovered

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (speedHighlighted) FocusRingColor else CardSurface)
                                    .hoverable(speedSource)
                                    .onFocusChanged { speedFocused = it.isFocused }
                                    .clickable(interactionSource = speedSource, indication = null) {
                                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val nextIdx = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                                        currentSpeed = speeds[nextIdx]
                                        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${currentSpeed}x",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (speedHighlighted) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Episode Browser Button (if series)
                            if (allSeasonEpisodes.isNotEmpty()) {
                                val epsSource = remember { MutableInteractionSource() }
                                var epsFocused by remember { mutableStateOf(false) }
                                val epsHovered by epsSource.collectIsHoveredAsState()
                                val epsHighlighted = epsFocused || epsHovered

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (epsHighlighted) FocusRingColor else CardSurface)
                                        .hoverable(epsSource)
                                        .onFocusChanged { epsFocused = it.isFocused }
                                        .clickable(interactionSource = epsSource, indication = null) {
                                            showEpisodesDrawer = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = "Episodes",
                                            tint = if (epsHighlighted) Color.White else TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Episodes",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (epsHighlighted) Color.White else TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            // Playlist Queue Button (if playlist)
                            if (allPlaylistItems.isNotEmpty()) {
                                val plSource = remember { MutableInteractionSource() }
                                var plFocused by remember { mutableStateOf(false) }
                                val plHovered by plSource.collectIsHoveredAsState()
                                val plHighlighted = plFocused || plHovered

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (plHighlighted) FocusRingColor else CardSurface)
                                        .hoverable(plSource)
                                        .onFocusChanged { plFocused = it.isFocused }
                                        .clickable(interactionSource = plSource, indication = null) {
                                            showPlaylistDrawer = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                            contentDescription = "Playlist Queue",
                                            tint = if (plHighlighted) Color.White else TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Queue (${allPlaylistItems.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (plHighlighted) Color.White else TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Center Transport Buttons: Previous, Rewind, Play/Pause, Forward, Next
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Playlist Item Button (if available)
                            if (previousPlaylistItem != null) {
                                val prevSource = remember { MutableInteractionSource() }
                                var prevFocused by remember { mutableStateOf(false) }
                                val prevHovered by prevSource.collectIsHoveredAsState()
                                val prevHighlighted = prevFocused || prevHovered

                                IconButton(
                                    onClick = { playPreviousPlaylistItemNow() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (prevHighlighted) FocusRingColor else CardSurface)
                                        .hoverable(prevSource)
                                        .onFocusChanged { prevFocused = it.isFocused }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Video",
                                        tint = if (prevHighlighted) Color.White else TextPrimary
                                    )
                                }
                            }

                            val rewindSource = remember { MutableInteractionSource() }
                            var rewindFocused by remember { mutableStateOf(false) }
                            val rewindHovered by rewindSource.collectIsHoveredAsState()
                            val rewindHighlighted = rewindFocused || rewindHovered

                            IconButton(
                                onClick = { triggerRewind() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (rewindHighlighted) FocusRingColor else CardSurface)
                                    .hoverable(rewindSource)
                                    .onFocusChanged { rewindFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = if (rewindHighlighted) Color.White else TextPrimary
                                )
                            }

                            val playSource = remember { MutableInteractionSource() }
                            var playFocused by remember { mutableStateOf(false) }
                            val playHovered by playSource.collectIsHoveredAsState()
                            val playHighlighted = playFocused || playHovered

                            IconButton(
                                onClick = { togglePlayPause() },
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(if (playHighlighted) FocusRingColor else JellyfinBlue)
                                    .hoverable(playSource)
                                    .focusRequester(playButtonFocus)
                                    .onFocusChanged { playFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            val fwdSource = remember { MutableInteractionSource() }
                            var fwdFocused by remember { mutableStateOf(false) }
                            val fwdHovered by fwdSource.collectIsHoveredAsState()
                            val fwdHighlighted = fwdFocused || fwdHovered

                            IconButton(
                                onClick = { triggerFastForward() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (fwdHighlighted) FocusRingColor else CardSurface)
                                    .hoverable(fwdSource)
                                    .onFocusChanged { fwdFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = if (fwdHighlighted) Color.White else TextPrimary
                                )
                            }

                            // Next Episode / Playlist Video Button (if available)
                            if (nextEpisode != null || nextPlaylistItem != null) {
                                val nextSource = remember { MutableInteractionSource() }
                                var nextFocused by remember { mutableStateOf(false) }
                                val nextHovered by nextSource.collectIsHoveredAsState()
                                val nextHighlighted = nextFocused || nextHovered

                                IconButton(
                                    onClick = {
                                        if (nextEpisode != null) {
                                            playNextEpisodeNow()
                                        } else {
                                            playNextPlaylistItemNow()
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (nextHighlighted) FocusRingColor else CardSurface)
                                        .hoverable(nextSource)
                                        .onFocusChanged { nextFocused = it.isFocused }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = if (nextEpisode != null) "Next Episode" else "Next Video",
                                        tint = if (nextHighlighted) Color.White else TextPrimary
                                    )
                                }
                            }
                        }

                        // Right Options: Subtitle Quick Toggle, Aspect Ratio, Full Tracks Dialog
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Subtitle Toggle Button
                            val ccSource = remember { MutableInteractionSource() }
                            var ccFocused by remember { mutableStateOf(false) }
                            val ccHovered by ccSource.collectIsHoveredAsState()
                            val ccHighlighted = ccFocused || ccHovered

                            IconButton(
                                onClick = { toggleSubtitles() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (ccHighlighted) FocusRingColor
                                        else if (isSubtitleActive) JellyfinBlue.copy(alpha = 0.45f)
                                        else CardSurface
                                    )
                                    .hoverable(ccSource)
                                    .onFocusChanged { ccFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = if (isSubtitleActive) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionOff,
                                    contentDescription = "Toggle Subtitles",
                                    tint = if (ccHighlighted || isSubtitleActive) Color.White else TextSecondary
                                )
                            }

                            val aspectSource = remember { MutableInteractionSource() }
                            var aspectFocused by remember { mutableStateOf(false) }
                            val aspectHovered by aspectSource.collectIsHoveredAsState()
                            val aspectHighlighted = aspectFocused || aspectHovered

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (aspectHighlighted) FocusRingColor else CardSurface)
                                    .hoverable(aspectSource)
                                    .onFocusChanged { aspectFocused = it.isFocused }
                                    .clickable(interactionSource = aspectSource, indication = null) {
                                        val modes = VideoAspectRatio.entries
                                        val nextIdx = (modes.indexOf(selectedAspectRatio) + 1) % modes.size
                                        selectedAspectRatio = modes[nextIdx]
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = selectedAspectRatio.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (aspectHighlighted) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            val tracksSource = remember { MutableInteractionSource() }
                            var tracksFocused by remember { mutableStateOf(false) }
                            val tracksHovered by tracksSource.collectIsHoveredAsState()
                            val tracksHighlighted = tracksFocused || tracksHovered

                            IconButton(
                                onClick = { showTrackDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (tracksHighlighted) FocusRingColor else CardSurface)
                                    .hoverable(tracksSource)
                                    .onFocusChanged { tracksFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Audio and Subtitles Settings",
                                    tint = if (tracksHighlighted) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    // Progress Track Bar & Scrubber
                    val positionMs = currentPosition.coerceAtLeast(0L)
                    val bufferedMs = bufferedPosition.coerceAtLeast(0L)
                    val totalDurationMs = duration.coerceAtLeast(1L)
                    val progressFraction = (positionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    val bufferFraction = (bufferedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    val remainingMs = (totalDurationMs - positionMs).coerceAtLeast(0L)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Scrubber timeline track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Unplayed / unbuffered base track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            // Buffered track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bufferFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.38f))
                            )

                            // Played track with glowing gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(JellyfinBlue, Color(0xFF38BDF8))
                                        )
                                    )
                            )

                            // Glowing scrubber cursor thumb
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .wrapContentWidth(Alignment.End)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, FocusRingColor, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Time Labels: Elapsed | Ends at HH:MM | -Remaining
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(positionMs),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            val endText = formatEndTime(remainingMs)
                            if (endText.isNotEmpty()) {
                                Text(
                                    text = endText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Text(
                                text = "-${formatTime(remainingMs)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // In-Player TV Episode Carousel Drawer
        if (showEpisodesDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { showEpisodesDrawer = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface)
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            IconButton(onClick = { showEpisodesDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = allSeasonEpisodes.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
                        )
                        val currentItemFocus = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            withFrameNanos { } // wait for the row to lay out its items
                            runCatching { currentItemFocus.requestFocus() }
                        }

                        LazyRow(
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(allSeasonEpisodes, key = { it.id }) { ep ->
                                val isCurrent = ep.id == media.id
                                val epSource = remember { MutableInteractionSource() }
                                var epFocused by remember { mutableStateOf(false) }
                                val epHovered by epSource.collectIsHoveredAsState()
                                val epHighlighted = epFocused || epHovered

                                Column(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .zIndex(if (epHighlighted) 10f else 0f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) JellyfinBlue.copy(alpha = 0.25f) else CardSurfaceVariant)
                                        .border(
                                            width = if (epHighlighted) 2.5.dp else if (isCurrent) 1.5.dp else 1.dp,
                                            color = if (epHighlighted) FocusRingColor else if (isCurrent) JellyfinBlue else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .hoverable(epSource)
                                        .onFocusChanged { epFocused = it.isFocused }
                                        .then(if (isCurrent) Modifier.focusRequester(currentItemFocus) else Modifier)
                                        .clickable(interactionSource = epSource, indication = null) {
                                            switchEpisode(ep)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        AuthenticatedAsyncImage(
                                            url = repository.getPosterUrl(ep.id),
                                            headers = imageHeaders,
                                            contentDescription = ep.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.8f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "E${ep.episodeIndex ?: 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = ep.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (epHighlighted) FocusRingColor else TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // In-Player Playlist Queue Drawer
        if (showPlaylistDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { showPlaylistDrawer = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface)
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = "Playlist Queue",
                                    tint = JellyfinBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Playlist Queue (${allPlaylistItems.size} Videos)",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            IconButton(onClick = { showPlaylistDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = allPlaylistItems.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
                        )
                        val currentItemFocus = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            withFrameNanos { } // wait for the row to lay out its items
                            runCatching { currentItemFocus.requestFocus() }
                        }

                        LazyRow(
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(allPlaylistItems, key = { it.id }) { item ->
                                val isCurrent = item.id == media.id
                                val itemSource = remember { MutableInteractionSource() }
                                var itemFocused by remember { mutableStateOf(false) }
                                val itemHovered by itemSource.collectIsHoveredAsState()
                                val itemHighlighted = itemFocused || itemHovered
                                val itemIndex = item.episodeIndex ?: (allPlaylistItems.indexOf(item) + 1)

                                Column(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .zIndex(if (itemHighlighted) 10f else 0f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) JellyfinBlue.copy(alpha = 0.25f) else CardSurfaceVariant)
                                        .border(
                                            width = if (itemHighlighted) 2.5.dp else if (isCurrent) 1.5.dp else 1.dp,
                                            color = if (itemHighlighted) FocusRingColor else if (isCurrent) JellyfinBlue else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .hoverable(itemSource)
                                        .onFocusChanged { itemFocused = it.isFocused }
                                        .then(if (isCurrent) Modifier.focusRequester(currentItemFocus) else Modifier)
                                        .clickable(interactionSource = itemSource, indication = null) {
                                            switchPlaylistItem(item)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        AuthenticatedAsyncImage(
                                            url = repository.getPosterUrl(item.id),
                                            headers = imageHeaders,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.8f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#$itemIndex",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(JellyfinBlue)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "NOW PLAYING",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (itemHighlighted) FocusRingColor else TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (item.durationMinutes > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.durationMinutes} min",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
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
    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { } // wait for the list to lay out its first item
        runCatching { firstItemFocus.requestFocus() }
    }

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

                    val offSource = remember { MutableInteractionSource() }
                    var offFocused by remember { mutableStateOf(false) }
                    val offHovered by offSource.collectIsHoveredAsState()
                    val offHighlighted = offFocused || offHovered

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (offHighlighted) FocusRingColor else CardSurfaceVariant)
                            .hoverable(offSource)
                            .onFocusChanged { offFocused = it.isFocused }
                            .focusRequester(firstItemFocus)
                            .clickable(interactionSource = offSource, indication = null) { onDisableSubtitles() }
                            .padding(12.dp)
                    ) {
                        Text("Off / Disabled", color = if (offHighlighted) Color.White else TextPrimary)
                    }
                }

                val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                items(textGroups) { group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val trackSource = remember { MutableInteractionSource() }
                        var isFocused by remember { mutableStateOf(false) }
                        val isHovered by trackSource.collectIsHoveredAsState()
                        val isHighlighted = isFocused || isHovered
                        val label = format.label ?: format.language ?: "Subtitle Track #${i + 1}"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHighlighted) FocusRingColor
                                    else if (isSelected) JellyfinBlue.copy(alpha = 0.4f)
                                    else CardSurfaceVariant
                                )
                                .hoverable(trackSource)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable(interactionSource = trackSource, indication = null) { onSelectTrack(group, i) }
                                .padding(12.dp)
                        ) {
                            Text(text = label, color = if (isHighlighted) Color.White else TextPrimary)
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
                        val trackSource = remember { MutableInteractionSource() }
                        var isFocused by remember { mutableStateOf(false) }
                        val isHovered by trackSource.collectIsHoveredAsState()
                        val isHighlighted = isFocused || isHovered
                        val label = format.label ?: format.language ?: "Audio Track #${i + 1}"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHighlighted) FocusRingColor
                                    else if (isSelected) JellyfinBlue.copy(alpha = 0.4f)
                                    else CardSurfaceVariant
                                )
                                .hoverable(trackSource)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable(interactionSource = trackSource, indication = null) { onSelectTrack(group, i) }
                                .padding(12.dp)
                        ) {
                            Text(text = label, color = if (isHighlighted) Color.White else TextPrimary)
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

private fun formatEndTime(remainingMs: Long): String {
    if (remainingMs <= 0L) return ""
    val endTime = System.currentTimeMillis() + remainingMs
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "Ends at ${sdf.format(Date(endTime))}"
}

