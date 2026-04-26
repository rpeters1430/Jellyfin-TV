package com.rpeters1430.jellyfintv

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rpeters1430.jellyfintv.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen video playback activity powered by ExoPlayer (Media3).
 *
 * Expects the following Intent extras:
 *  - [EXTRA_STREAM_URL] – The direct-stream or HLS URL.
 *  - [EXTRA_ITEM_ID]    – Jellyfin item ID (for progress reporting).
 *  - [EXTRA_PLAY_SESSION_ID] – Optional play session ID from PlaybackInfo.
 *  - [EXTRA_START_POSITION_MS] – Resume position in milliseconds (optional).
 */
class PlaybackActivity : FragmentActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private val prefs by lazy { (application as App).prefs }
    private val repository by lazy { (application as App).repository }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var itemId: String? = null
    private var playSessionId: String? = null
    private var progressJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)

        playerView = findViewById(R.id.player_view)

        itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        playSessionId = intent.getStringExtra(EXTRA_PLAY_SESSION_ID)
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        val startPositionMs = intent.getLongExtra(EXTRA_START_POSITION_MS, 0L)

        if (streamUrl.isNullOrBlank()) {
            finish()
            return
        }

        initPlayer(streamUrl, startPositionMs)
    }

    private fun initPlayer(streamUrl: String, startPositionMs: Long) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.seekTo(startPositionMs)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        reportStopped()
                        finish()
                    }
                }
            })
        }

        startProgressReporting()
    }

    private fun startProgressReporting() {
        progressJob = scope.launch {
            while (true) {
                delay(PROGRESS_REPORT_INTERVAL_MS)
                val positionMs = player.currentPosition
                val id = itemId ?: break
                // Convert ms → 100-nanosecond ticks
                repository.reportPlaybackProgress(
                    itemId = id,
                    positionTicks = positionMs * 10_000L,
                    playSessionId = playSessionId
                )
            }
        }
    }

    private fun reportStopped() {
        val id = itemId ?: return
        scope.launch {
            repository.reportPlaybackStopped(
                itemId = id,
                positionTicks = player.currentPosition * 10_000L,
                playSessionId = playSessionId
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return playerView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return playerView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            || super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onResume() {
        super.onResume()
        player.play()
    }

    override fun onDestroy() {
        reportStopped()
        progressJob?.cancel()
        scope.cancel()
        player.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_PLAY_SESSION_ID = "extra_play_session_id"
        const val EXTRA_START_POSITION_MS = "extra_start_position_ms"
        private const val PROGRESS_REPORT_INTERVAL_MS = 10_000L
    }
}
