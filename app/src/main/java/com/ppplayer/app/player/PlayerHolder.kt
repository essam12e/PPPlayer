package com.ppplayer.app.player

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.ppplayer.app.PPPlayerApp
import com.ppplayer.app.data.DecoderMode
import com.ppplayer.app.data.HistoryEntry
import com.ppplayer.app.data.Prefs
import com.ppplayer.app.util.resumablePosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One entry of the play queue handed to the player. */
data class QueueItem(val uri: String, val title: String, val isAudio: Boolean)

/** Sleep timer state: off, a wall-clock deadline, or "stop when the current item ends". */
sealed interface SleepTimer {
    data object Off : SleepTimer
    data class Until(val deadlineMs: Long, val minutes: Int) : SleepTimer
    data object EndOfItem : SleepTimer
}

/**
 * Owns the single ExoPlayer instance shared by the player screen, the background
 * playback service and the mini player on the home screen.
 */
@OptIn(UnstableApi::class)
object PlayerHolder {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _sleep = MutableStateFlow<SleepTimer>(SleepTimer.Off)
    val sleep: StateFlow<SleepTimer> = _sleep.asStateFlow()

    /** Set while the player screen is on the back stack. */
    var activityAttached = false

    /** Called by the service so its session follows a rebuilt player. */
    var onPlayerReplaced: ((ExoPlayer) -> Unit)? = null

    /** Called before the player is released so the session can be torn down first. */
    var onBeforeRelease: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        _player.value?.pause()
        _sleep.value = SleepTimer.Off
    }
    private var enhancer: LoudnessEnhancer? = null
    private var enhancerSession = C.AUDIO_SESSION_ID_UNSET
    private lateinit var prefs: Prefs

    private fun prefsFor(context: Context): Prefs {
        if (!::prefs.isInitialized) prefs = (context.applicationContext as PPPlayerApp).prefs
        return prefs
    }

    fun ensure(context: Context): ExoPlayer = _player.value ?: build(context).also { _player.value = it }

    private fun build(context: Context): ExoPlayer {
        val app = context.applicationContext
        val settings = prefsFor(app).settings.value
        val renderers = DefaultRenderersFactory(app)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                when (settings.decoder) {
                    DecoderMode.HW -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    DecoderMode.HW_PLUS -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    DecoderMode.SW -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                }
            )
        val step = settings.seekStepSec * 1000L
        return ExoPlayer.Builder(app, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(step)
            .setSeekForwardIncrementMs(step)
            .build()
            .also { p ->
                p.addListener(persistence)
                applyPauseAtEnd(p)
            }
    }

    /** Replaces the queue and starts playback at [startIndex]. */
    fun play(context: Context, items: List<QueueItem>, startIndex: Int) {
        if (items.isEmpty()) return
        val p = ensure(context)
        saveCurrent(p)
        val index = startIndex.coerceIn(0, items.lastIndex)
        val settings = prefsFor(context).settings.value
        val startPos = if (settings.resume) {
            val saved = prefs.history.value.firstOrNull { it.uri == items[index].uri }
            if (saved != null) resumablePosition(saved.positionMs, saved.durationMs) else 0L
        } else 0L
        p.setMediaItems(items.map { it.toMediaItem() }, index, startPos)
        p.setPlaybackSpeed(1f)
        p.repeatMode = Player.REPEAT_MODE_OFF
        p.shuffleModeEnabled = false
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverrides()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        setBoost(0f)
        applyPauseAtEnd(p)
        p.prepare()
        p.playWhenReady = true
    }

    /** Recreates the player (after a decoder change) while keeping queue and position. */
    fun rebuild(context: Context) {
        val old = _player.value ?: return
        val items = List(old.mediaItemCount) { old.getMediaItemAt(it) }
        val index = old.currentMediaItemIndex
        val position = old.currentPosition
        val playWhenReady = old.playWhenReady
        val speed = old.playbackParameters.speed
        val repeat = old.repeatMode
        val shuffle = old.shuffleModeEnabled
        releaseEnhancer()
        val fresh = build(context)
        if (items.isNotEmpty()) {
            fresh.setMediaItems(items, index, position)
            fresh.setPlaybackSpeed(speed)
            fresh.repeatMode = repeat
            fresh.shuffleModeEnabled = shuffle
            fresh.prepare()
            fresh.playWhenReady = playWhenReady
        }
        _player.value = fresh
        onPlayerReplaced?.invoke(fresh)
        old.removeListener(persistence)
        old.release()
    }

    fun release() {
        val p = _player.value ?: return
        saveCurrent(p)
        setSleep(SleepTimer.Off)
        onBeforeRelease?.invoke()
        releaseEnhancer()
        _player.value = null
        p.removeListener(persistence)
        p.release()
    }

    // ---- Sleep timer -------------------------------------------------------------------------

    fun setSleep(timer: SleepTimer) {
        handler.removeCallbacks(sleepRunnable)
        _sleep.value = timer
        if (timer is SleepTimer.Until) {
            handler.postDelayed(sleepRunnable, (timer.deadlineMs - System.currentTimeMillis()).coerceAtLeast(0))
        }
        _player.value?.let(::applyPauseAtEnd)
    }

    /** Re-reads the settings that live on the player instance. */
    fun applySettings() {
        _player.value?.let(::applyPauseAtEnd)
    }

    private fun applyPauseAtEnd(p: ExoPlayer) {
        val autoNext = if (::prefs.isInitialized) prefs.settings.value.autoPlayNext else true
        p.pauseAtEndOfMediaItems = !autoNext || _sleep.value is SleepTimer.EndOfItem
    }

    // ---- Volume boost ------------------------------------------------------------------------

    /** Extra gain above 100% volume; [fraction] 0..1 maps to 0..+15 dB. */
    fun setBoost(fraction: Float) {
        val p = _player.value ?: return
        val gain = (fraction.coerceIn(0f, 1f) * MAX_BOOST_MB).toInt()
        runCatching {
            val session = p.audioSessionId
            if (gain == 0 && enhancer == null) return
            if (enhancer == null || enhancerSession != session) {
                releaseEnhancer()
                enhancer = LoudnessEnhancer(session)
                enhancerSession = session
            }
            enhancer?.setTargetGain(gain)
            enhancer?.enabled = gain > 0
        }
    }

    private fun releaseEnhancer() {
        runCatching { enhancer?.release() }
        enhancer = null
        enhancerSession = C.AUDIO_SESSION_ID_UNSET
    }

    // ---- Position persistence ----------------------------------------------------------------

    private val durations = HashMap<String, Long>()

    private val persistence = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) _player.value?.let(::saveCurrent)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            val oldItem = oldPosition.mediaItem ?: return
            if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex && oldItem == newPosition.mediaItem) return
            val finished = reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
            store(oldItem, if (finished) 0L else oldPosition.positionMs, durations[oldItem.mediaId] ?: 0L)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val p = _player.value ?: return
            if (mediaItem == null || !::prefs.isInitialized || !prefs.settings.value.resume) return
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && reason != Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) return
            val saved = prefs.history.value.firstOrNull { it.uri == mediaItem.mediaId } ?: return
            val pos = resumablePosition(saved.positionMs, saved.durationMs)
            if (pos > 0) p.seekTo(pos)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val p = _player.value ?: return
            if (playbackState == Player.STATE_READY) {
                val item = p.currentMediaItem ?: return
                if (p.duration != C.TIME_UNSET) durations[item.mediaId] = p.duration
            } else if (playbackState == Player.STATE_ENDED) {
                p.currentMediaItem?.let { store(it, 0L, durations[it.mediaId] ?: 0L) }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && _sleep.value is SleepTimer.EndOfItem) {
                setSleep(SleepTimer.Off)
            }
        }
    }

    fun saveCurrent(p: Player) {
        val item = p.currentMediaItem ?: return
        if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) return
        val duration = p.duration.takeIf { it != C.TIME_UNSET } ?: (durations[item.mediaId] ?: 0L)
        store(item, p.currentPosition, duration)
    }

    private fun store(item: MediaItem, positionMs: Long, durationMs: Long) {
        if (!::prefs.isInitialized) return
        prefs.record(
            HistoryEntry(
                uri = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                positionMs = resumablePosition(positionMs, durationMs),
                durationMs = durationMs,
                isAudio = item.isAudio,
                playedAt = System.currentTimeMillis(),
            )
        )
    }

    private const val MAX_BOOST_MB = 1500f
}

fun QueueItem.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaId(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setMediaType(if (isAudio) MediaMetadata.MEDIA_TYPE_MUSIC else MediaMetadata.MEDIA_TYPE_VIDEO)
                .build()
        )
        .build()

val MediaItem.isAudio: Boolean
    get() = mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_MUSIC
