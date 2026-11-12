package com.ppplayer.app.player

import android.content.Context
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.ppplayer.app.R
import com.ppplayer.app.data.Prefs
import com.ppplayer.app.data.Settings
import com.ppplayer.app.ui.theme.Brand
import com.ppplayer.app.ui.theme.TABULAR
import com.ppplayer.app.util.formatDuration
import com.ppplayer.app.util.formatSignedDuration
import com.ppplayer.app.util.formatSpeed
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AspectMode(val labelRes: Int) {
    FIT(R.string.aspect_fit),
    CROP(R.string.aspect_crop),
    STRETCH(R.string.aspect_stretch),
    R16_9(R.string.aspect_16_9),
    R4_3(R.string.aspect_4_3),
}

enum class Panel { NONE, MORE, AUDIO, SUBTITLES, SPEED, ASPECT, SLEEP, REPEAT, DECODER, QUEUE, INFO }

/** Snapshot of the player that Compose can observe. */
@Stable
class PlayerUiState(private val player: Player) : Player.Listener {
    var isPlaying by mutableStateOf(player.isPlaying)
    var playbackState by mutableIntStateOf(player.playbackState)
    var title by mutableStateOf(player.mediaMetadata.title?.toString().orEmpty())
    var mediaId by mutableStateOf(player.currentMediaItem?.mediaId.orEmpty())
    var isAudioItem by mutableStateOf(player.currentMediaItem?.isAudio == true)
    var durationMs by mutableLongStateOf(0L)
    var positionMs by mutableLongStateOf(0L)
    var bufferedMs by mutableLongStateOf(0L)
    var videoSize by mutableStateOf(player.videoSize)
    var tracks by mutableStateOf(player.currentTracks)
    var speed by mutableFloatStateOf(player.playbackParameters.speed)
    var repeatMode by mutableIntStateOf(player.repeatMode)
    var shuffle by mutableStateOf(player.shuffleModeEnabled)
    var hasNext by mutableStateOf(player.hasNextMediaItem())
    var hasPrevious by mutableStateOf(player.hasPreviousMediaItem())
    var index by mutableIntStateOf(player.currentMediaItemIndex)
    var count by mutableIntStateOf(player.mediaItemCount)
    var error by mutableStateOf<PlaybackException?>(player.playerError)
    var cues by mutableStateOf<List<Cue>>(emptyList())
    var textDisabled by mutableStateOf(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))

    val hasVideo: Boolean
        get() = tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }

    init {
        tick()
    }

    fun tick() {
        positionMs = player.currentPosition.coerceAtLeast(0)
        bufferedMs = player.bufferedPosition.coerceAtLeast(0)
        durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0L
    }

    override fun onEvents(player: Player, events: Player.Events) {
        isPlaying = player.isPlaying
        playbackState = player.playbackState
        title = player.mediaMetadata.title?.toString().orEmpty()
        mediaId = player.currentMediaItem?.mediaId.orEmpty()
        isAudioItem = player.currentMediaItem?.isAudio == true
        tracks = player.currentTracks
        speed = player.playbackParameters.speed
        repeatMode = player.repeatMode
        shuffle = player.shuffleModeEnabled
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()
        index = player.currentMediaItemIndex
        count = player.mediaItemCount
        error = player.playerError
        textDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        tick()
    }

    override fun onVideoSizeChanged(size: VideoSize) {
        videoSize = size
    }

    override fun onCues(cueGroup: CueGroup) {
        cues = cueGroup.cues
    }
}

private enum class DragMode { NONE, SEEK, BRIGHTNESS, VOLUME, IGNORED }

private sealed interface Feedback {
    data class Seek(val targetMs: Long, val deltaMs: Long) : Feedback
    data class Level(val icon: ImageVector, val labelRes: Int, val percent: Int, val max: Int) : Feedback
    data class Jump(val forward: Boolean, val seconds: Int) : Feedback
    data class Text(val text: String) : Feedback
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    player: ExoPlayer,
    settings: Settings,
    prefs: Prefs,
    inPip: Boolean,
    subtitleLoadedSignal: Int,
    host: PlayerHost,
) {
    val context = LocalContext.current
    val state = remember(player) { PlayerUiState(player) }
    DisposableEffect(player) {
        player.addListener(state)
        onDispose { player.removeListener(state) }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf(Panel.NONE) }
    var aspect by remember { mutableStateOf(AspectMode.FIT) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var abStart by remember(state.mediaId) { mutableStateOf<Long?>(null) }
    var abEnd by remember(state.mediaId) { mutableStateOf<Long?>(null) }
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var interaction by remember { mutableIntStateOf(0) }
    var holdingSpeed by remember { mutableStateOf(false) }

    val showArtwork = state.isAudioItem || (state.playbackState == Player.STATE_READY && !state.hasVideo)

    // Position ticker + A–B loop.
    LaunchedEffect(player) {
        while (true) {
            state.tick()
            val a = abStart
            val b = abEnd
            if (a != null && b != null && state.positionMs >= b) player.seekTo(a)
            delay(250)
        }
    }
    LaunchedEffect(state.isPlaying) { host.keepScreenOn(state.isPlaying) }
    LaunchedEffect(state.mediaId, state.videoSize.width > 0, state.isAudioItem) {
        host.applyOrientation(state.videoSize.width, state.videoSize.height, state.isAudioItem)
        zoom = 1f
    }
    LaunchedEffect(controlsVisible, state.isPlaying, panel, interaction, showArtwork) {
        if (controlsVisible && state.isPlaying && panel == Panel.NONE && !showArtwork) {
            delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(700)
            feedback = null
        }
    }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2200)
            hint = null
        }
    }
    LaunchedEffect(lockHintVisible) {
        if (lockHintVisible) {
            delay(2500)
            lockHintVisible = false
        }
    }
    val subtitleLoadedText = stringResource(R.string.player_subtitle_loaded)
    LaunchedEffect(subtitleLoadedSignal) {
        if (subtitleLoadedSignal > 0) hint = subtitleLoadedText
    }

    BackHandler {
        when {
            panel != Panel.NONE -> panel = Panel.NONE
            locked -> lockHintVisible = true
            else -> host.exit()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        VideoSurface(player, state.videoSize, aspect, zoom)

        if (showArtwork && !inPip) AudioArtwork(state.title)

        SubtitleLayer(state.cues, settings.subtitleScale)

        if (inPip) return@Box

        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        val currentSettings by rememberUpdatedState(settings)
        var volumeLevel by remember { mutableFloatStateOf(readVolume(audioManager)) }

        // Gesture surface.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (currentSettings.gestureZoom && event.changes.count { it.pressed } >= 2) {
                                val factor = event.calculateZoom()
                                if (factor != 1f) {
                                    zoom = (zoom * factor).coerceIn(1f, 4f)
                                    feedback = Feedback.Text(
                                        context.getString(R.string.player_zoom, (zoom * 100).roundToInt())
                                    )
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = {
                            if (locked) lockHintVisible = true else controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            if (locked) return@detectTapGestures
                            val third = size.width / 3f
                            val step = currentSettings.seekStepSec
                            when {
                                !currentSettings.gestureDoubleTap || (offset.x >= third && offset.x <= 2 * third) -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                }
                                offset.x < third -> {
                                    player.seekBack()
                                    feedback = Feedback.Jump(false, step)
                                }
                                else -> {
                                    player.seekForward()
                                    feedback = Feedback.Jump(true, step)
                                }
                            }
                        },
                        onLongPress = {
                            if (!locked && currentSettings.gestureHoldSpeed && player.isPlaying) {
                                holdingSpeed = true
                                player.setPlaybackSpeed(2f)
                            }
                        },
                        onPress = {
                            val before = player.playbackParameters.speed
                            tryAwaitRelease()
                            if (holdingSpeed) {
                                holdingSpeed = false
                                player.setPlaybackSpeed(before)
                            }
                        },
                    )
                }
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    var mode = DragMode.NONE
                    var total = Offset.Zero
                    var start = Offset.Zero
                    var startPosition = 0L
                    var startBrightness = 0f
                    var startVolume = 0f
                    var seekTarget = 0L
                    val edge = 32.dp.toPx()
                    detectDragGestures(
                        onDragStart = { offset ->
                            start = offset
                            total = Offset.Zero
                            mode = if (offset.y < edge || offset.y > size.height - edge) DragMode.IGNORED else DragMode.NONE
                            startPosition = player.currentPosition
                            startBrightness = host.brightness()
                            volumeLevel = readVolume(audioManager).let { v -> if (v >= 1f) maxOf(v, volumeLevel) else v }
                            startVolume = volumeLevel
                        },
                        onDragEnd = {
                            if (mode == DragMode.SEEK) player.seekTo(seekTarget)
                            mode = DragMode.NONE
                        },
                        onDragCancel = { mode = DragMode.NONE },
                        onDrag = { change, amount ->
                            total += amount
                            val s = currentSettings
                            if (mode == DragMode.NONE) {
                                mode = when {
                                    abs(total.x) > abs(total.y) ->
                                        if (s.gestureSeek && player.isCurrentMediaItemSeekable && state.durationMs > 0) DragMode.SEEK
                                        else DragMode.IGNORED
                                    start.x < size.width / 2f -> if (s.gestureBrightness) DragMode.BRIGHTNESS else DragMode.IGNORED
                                    else -> if (s.gestureVolume) DragMode.VOLUME else DragMode.IGNORED
                                }
                            }
                            when (mode) {
                                DragMode.SEEK -> {
                                    val duration = state.durationMs
                                    val range = (duration / 3).coerceIn(minOf(60_000L, duration), 600_000L)
                                    val delta = (total.x / size.width * range).toLong()
                                    seekTarget = (startPosition + delta).coerceIn(0L, duration)
                                    feedback = Feedback.Seek(seekTarget, seekTarget - startPosition)
                                    change.consume()
                                }
                                DragMode.BRIGHTNESS -> {
                                    val value = (startBrightness - total.y / size.height * 1.3f).coerceIn(0.01f, 1f)
                                    host.setBrightness(value)
                                    feedback = Feedback.Level(
                                        Icons.Rounded.BrightnessMedium, R.string.player_brightness, (value * 100).roundToInt(), 100
                                    )
                                    change.consume()
                                }
                                DragMode.VOLUME -> {
                                    val value = (startVolume - total.y / size.height * 1.6f).coerceIn(0f, 2f)
                                    volumeLevel = value
                                    writeVolume(audioManager, value)
                                    PlayerHolder.setBoost((value - 1f).coerceAtLeast(0f))
                                    feedback = Feedback.Level(
                                        if (value <= 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                        R.string.player_volume, (value * 100).roundToInt(), 200
                                    )
                                    change.consume()
                                }
                                else -> Unit
                            }
                        },
                    )
                }
        )

        if (state.playbackState == Player.STATE_BUFFERING && state.error == null) {
            CircularProgressIndicator(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp),
                color = Color.White,
                strokeWidth = 3.dp,
            )
        }

        FeedbackOverlay(feedback, holdingSpeed)

        // Controls.
        AnimatedVisibility(
            visible = controlsVisible && !locked && state.error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerControls(
                state = state,
                aspect = aspect,
                scrubMs = scrubMs,
                abStart = abStart,
                abEnd = abEnd,
                canPip = host.canPip && !showArtwork,
                showVideoTools = !showArtwork,
                onInteraction = { interaction++ },
                onBack = host::exit,
                onPanel = { panel = it },
                onPlayPause = { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() } },
                onNext = { player.seekToNextMediaItem() },
                onPrevious = { player.seekToPreviousMediaItem() },
                onScrub = { scrubMs = it },
                onScrubEnd = {
                    player.seekTo(it)
                    scrubMs = null
                },
                onLock = {
                    locked = true
                    lockHintVisible = true
                },
                onRotate = host::toggleOrientation,
                onAspect = {
                    aspect = AspectMode.entries[(aspect.ordinal + 1) % AspectMode.entries.size]
                    zoom = 1f
                    feedback = Feedback.Text(context.getString(aspect.labelRes))
                },
                onPip = host::enterPip,
            )
        }

        // Lock overlay.
        AnimatedVisibility(
            visible = locked && lockHintVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(24.dp),
        ) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Brand.colors.scrim)
                    .clickable {
                        locked = false
                        controlsVisible = true
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.LockOpen, null, tint = Color.White)
                Text(stringResource(R.string.player_unlock), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Transient hint.
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
        ) {
            Text(
                hint.orEmpty(),
                Modifier
                    .clip(CircleShape)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.error?.let {
            ErrorPanel(
                onRetry = {
                    player.prepare()
                    player.play()
                },
                onDecoder = { panel = Panel.DECODER },
                onClose = host::exit,
            )
        }

        PlayerPanelHost(
            panel = panel,
            onPanel = { panel = it },
            player = player,
            state = state,
            settings = settings,
            prefs = prefs,
            aspect = aspect,
            onAspect = {
                aspect = it
                zoom = 1f
            },
            abActive = abStart != null,
            onAbRepeat = {
                when {
                    abStart == null -> {
                        abStart = state.positionMs
                        hint = context.getString(R.string.ab_set_a)
                    }
                    abEnd == null && state.positionMs > abStart!! + 500 -> {
                        abEnd = state.positionMs
                        hint = context.getString(R.string.ab_set_b)
                    }
                    else -> {
                        abStart = null
                        abEnd = null
                        hint = context.getString(R.string.ab_cleared)
                    }
                }
                panel = Panel.NONE
            },
            onHint = { hint = it },
            onPickSubtitle = {
                panel = Panel.NONE
                host.pickSubtitle()
            },
        )
    }
}

private fun readVolume(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun writeVolume(audio: AudioManager, level: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val index = (level.coerceIn(0f, 1f) * max).roundToInt()
    runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0) }
}

@Composable
private fun VideoSurface(player: ExoPlayer, videoSize: VideoSize, aspect: AspectMode, zoom: Float) {
    val context = LocalContext.current
    val surface = remember { SurfaceView(context) }
    DisposableEffect(player, surface) {
        player.setVideoSurfaceView(surface)
        onDispose { player.clearVideoSurfaceView(surface) }
    }
    val videoAspect =
        if (videoSize.width > 0 && videoSize.height > 0) {
            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
        } else 16f / 9f
    AndroidView(
        factory = { surface },
        modifier = Modifier.layout { measurable, constraints ->
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()
            val target = when (aspect) {
                AspectMode.FIT, AspectMode.CROP -> videoAspect
                AspectMode.STRETCH -> cw / ch
                AspectMode.R16_9 -> 16f / 9f
                AspectMode.R4_3 -> 4f / 3f
            }
            val containerWider = cw / ch > target
            val fillWidth = if (aspect == AspectMode.CROP) containerWider else !containerWider
            val w = (if (fillWidth) cw else ch * target) * zoom
            val h = (if (fillWidth) cw / target else ch) * zoom
            val placeable = measurable.measure(
                Constraints.fixed(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1))
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(
                    (constraints.maxWidth - placeable.width) / 2,
                    (constraints.maxHeight - placeable.height) / 2,
                )
            }
        },
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun SubtitleLayer(cues: List<Cue>, scale: Float) {
    AndroidView(
        factory = { ctx ->
            SubtitleView(ctx).apply {
                setStyle(
                    CaptionStyleCompat(
                        AndroidColor.WHITE,
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        AndroidColor.BLACK,
                        null,
                    )
                )
                setApplyEmbeddedStyles(true)
                setApplyEmbeddedFontSizes(false)
                setBottomPaddingFraction(0.07f)
            }
        },
        update = { view ->
            view.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * scale)
            view.setCues(cues)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun AudioArtwork(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Brand.colors.glow.copy(alpha = 0.55f), Color(0xFF0A0C16)),
                    radius = 900f,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Image(
                painterResource(R.drawable.brand_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(34.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.now_playing),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FeedbackOverlay(feedback: Feedback?, holdingSpeed: Boolean) {
    Box(Modifier.fillMaxSize()) {
        if (holdingSpeed) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .clip(CircleShape)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.FastForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.player_speed_boost), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        when (feedback) {
            is Feedback.Seek -> Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    formatDuration(feedback.targetMs),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = TABULAR),
                )
                Text(
                    formatSignedDuration(feedback.deltaMs),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR),
                )
            }
            is Feedback.Level -> Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .width(132.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(feedback.icon, stringResource(feedback.labelRes), tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "${feedback.percent}%",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR),
                )
                Spacer(Modifier.height(10.dp))
                val gradient = Brand.colors.gradient
                val boosted = feedback.percent > 100
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                ) {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(Color.White.copy(alpha = 0.22f), cornerRadius = radius)
                    val fraction = (feedback.percent.toFloat() / feedback.max).coerceIn(0f, 1f)
                    if (boosted) {
                        drawRoundRect(Color(0xFFFFB45C), size = Size(size.width * fraction, size.height), cornerRadius = radius)
                    } else {
                        drawRoundRect(gradient, size = Size(size.width * fraction, size.height), cornerRadius = radius)
                    }
                }
            }
            is Feedback.Jump -> Row(
                Modifier
                    .align(if (feedback.forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 56.dp)
                    .clip(CircleShape)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (feedback.forward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    stringResource(if (feedback.forward) R.string.player_forward else R.string.player_rewind),
                    tint = Color.White,
                )
                Text(
                    (if (feedback.forward) "+" else "-") + feedback.seconds + "s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR),
                )
            }
            is Feedback.Text -> Text(
                feedback.text,
                Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Brand.colors.scrim)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            null -> Unit
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    aspect: AspectMode,
    scrubMs: Long?,
    abStart: Long?,
    abEnd: Long?,
    canPip: Boolean,
    showVideoTools: Boolean,
    onInteraction: () -> Unit,
    onBack: () -> Unit,
    onPanel: (Panel) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onAspect: () -> Unit,
    onPip: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // Scrims keep the white controls readable over bright frames.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
        )

        // Top bar.
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlIcon(Icons.AutoMirrored.Rounded.ArrowBack, R.string.back, onClick = onBack)
            Text(
                state.title,
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .basicMarquee(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            if (showVideoTools) {
                ControlIcon(Icons.Rounded.ClosedCaption, R.string.player_subtitles) { onPanel(Panel.SUBTITLES) }
            }
            ControlIcon(Icons.Rounded.Audiotrack, R.string.player_audio_track) { onPanel(Panel.AUDIO) }
            ControlIcon(Icons.Rounded.MoreVert, R.string.more) { onPanel(Panel.MORE) }
        }

        // Transport and the time bar stay left-to-right in every language, like the media itself.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ControlIcon(
                    Icons.Rounded.SkipPrevious, R.string.player_previous,
                    onClick = {
                        onInteraction()
                        onPrevious()
                    },
                    size = 52.dp, iconSize = 34.dp, enabled = state.hasPrevious || state.positionMs > 3000,
                )
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brand.colors.gradient)
                        .clickable {
                            onInteraction()
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                        tint = Color.White,
                        modifier = Modifier.size(42.dp),
                    )
                }
                ControlIcon(
                    Icons.Rounded.SkipNext, R.string.player_next,
                    onClick = {
                        onInteraction()
                        onNext()
                    },
                    size = 52.dp, iconSize = 34.dp, enabled = state.hasNext,
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
            ) {
                val shown = scrubMs ?: state.positionMs
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeText(formatDuration(shown))
                    SeekBar(
                        positionMs = shown,
                        bufferedMs = state.bufferedMs,
                        durationMs = state.durationMs,
                        abStart = abStart,
                        abEnd = abEnd,
                        onScrub = {
                            onInteraction()
                            onScrub(it)
                        },
                        onScrubEnd = onScrubEnd,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    TimeText(if (state.durationMs > 0) formatDuration(state.durationMs) else "--:--")
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ControlIcon(Icons.Rounded.Lock, R.string.player_lock, onClick = onLock)
                        if (showVideoTools) {
                            ControlIcon(Icons.Rounded.ScreenRotation, R.string.player_rotate) {
                                onInteraction()
                                onRotate()
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpeedChip(state.speed) { onPanel(Panel.SPEED) }
                        if (showVideoTools) {
                            ControlIcon(
                                Icons.Rounded.AspectRatio, R.string.player_aspect,
                                onClick = {
                                    onInteraction()
                                    onAspect()
                                },
                                tint = if (aspect == AspectMode.FIT) Color.White else MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (canPip) ControlIcon(Icons.Rounded.PictureInPictureAlt, R.string.player_pip, onClick = onPip)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeText(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
    )
}

@Composable
private fun SpeedChip(speed: Float, onClick: () -> Unit) {
    val label = stringResource(R.string.player_speed)
    Text(
        formatSpeed(speed),
        Modifier
            .padding(horizontal = 6.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(if (speed == 1f) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
        color = Color.White,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    labelRes: Int,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    enabled: Boolean = true,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size)) {
        Icon(
            icon,
            stringResource(labelRes),
            tint = if (enabled) tint else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Thin gradient time bar with a buffered track and optional A–B markers. */
@Composable
private fun SeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    abStart: Long?,
    abEnd: Long?,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brand.colors.gradient
    val enabled = durationMs > 0
    val duration by rememberUpdatedState(durationMs)
    val scrub by rememberUpdatedState(onScrub)
    val scrubEnd by rememberUpdatedState(onScrubEnd)
    var dragMs by remember { mutableLongStateOf(0L) }
    val fraction = if (enabled) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Canvas(
        modifier
            .height(44.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { value ->
                    if (duration <= 0) false else {
                        scrubEnd((value * duration).toLong())
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    scrubEnd((offset.x / size.width).coerceIn(0f, 1f).times(duration).toLong())
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragMs = (offset.x / size.width).coerceIn(0f, 1f).times(duration).toLong()
                        scrub(dragMs)
                    },
                    onDragEnd = { scrubEnd(dragMs) },
                    onDragCancel = { scrubEnd(dragMs) },
                    onHorizontalDrag = { change, _ ->
                        dragMs = (change.position.x / size.width).coerceIn(0f, 1f).times(duration).toLong()
                        scrub(dragMs)
                        change.consume()
                    },
                )
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val y = (size.height - trackHeight) / 2
        val radius = CornerRadius(trackHeight / 2)
        drawRoundRect(Color.White.copy(alpha = 0.24f), Offset(0f, y), Size(size.width, trackHeight), radius)
        if (enabled) {
            val buffered = (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
            drawRoundRect(Color.White.copy(alpha = 0.38f), Offset(0f, y), Size(size.width * buffered, trackHeight), radius)
            drawRoundRect(gradient, Offset(0f, y), Size(size.width * fraction, trackHeight), radius)
            listOfNotNull(abStart, abEnd).forEach { marker ->
                val x = size.width * (marker.toFloat() / durationMs).coerceIn(0f, 1f)
                drawRoundRect(
                    Color(0xFFFFD166),
                    Offset(x - 1.5.dp.toPx(), y - 5.dp.toPx()),
                    Size(3.dp.toPx(), trackHeight + 10.dp.toPx()),
                    CornerRadius(1.5.dp.toPx()),
                )
            }
            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(size.width * fraction, size.height / 2))
        }
    }
}

@Composable
private fun ErrorPanel(onRetry: () -> Unit, onDecoder: () -> Unit, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .width(360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.player_error),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.player_error_body),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDecoder) { Text(stringResource(R.string.player_decoder)) }
                Button(onClick = onRetry, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.player_retry))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        }
    }
}

/** True when the layout is wider than it is tall; panels dock to the side then. */
@Composable
internal fun IsWide(content: @Composable (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) { content(maxWidth > maxHeight) }
}
