package com.ppplayer.app.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ppplayer.app.R
import com.ppplayer.app.data.DecoderMode
import com.ppplayer.app.data.Prefs
import com.ppplayer.app.data.Settings
import com.ppplayer.app.ui.theme.TABULAR
import com.ppplayer.app.util.formatDuration
import com.ppplayer.app.util.formatSpeed
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun PlayerPanelHost(
    panel: Panel,
    onPanel: (Panel) -> Unit,
    player: ExoPlayer,
    state: PlayerUiState,
    settings: Settings,
    prefs: Prefs,
    aspect: AspectMode,
    onAspect: (AspectMode) -> Unit,
    abActive: Boolean,
    onAbRepeat: () -> Unit,
    onHint: (String) -> Unit,
    onPickSubtitle: () -> Unit,
) {
    val open = panel != Panel.NONE
    // Remember the last panel so its content stays visible while sliding out.
    val last = remember { arrayOf(Panel.MORE) }
    if (open) last[0] = panel
    val shown = if (open) panel else last[0]

    IsWide { wide ->
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onPanel(Panel.NONE) }
                )
            }
            AnimatedVisibility(
                visible = open,
                enter = if (wide) slideInHorizontally { it } + fadeIn() else slideInVertically { it } + fadeIn(),
                exit = if (wide) slideOutHorizontally { it } + fadeOut() else slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(if (wide) Alignment.CenterEnd else Alignment.BottomCenter),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = if (wide) RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
                    else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = if (wide) Modifier
                        .fillMaxHeight()
                        .width(348.dp)
                    else Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                ) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.displayCutout)
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        PanelHeader(
                            title = stringResource(shown.titleRes()),
                            showBack = shown != Panel.MORE && shown != Panel.AUDIO && shown != Panel.SUBTITLES,
                            onBack = { onPanel(Panel.MORE) },
                        )
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp)
                        ) {
                            when (shown) {
                                Panel.MORE -> MorePanel(player, state, settings, prefs, abActive, onPanel, onAbRepeat)
                                Panel.AUDIO -> TrackPanel(player, state.tracks, C.TRACK_TYPE_AUDIO)
                                Panel.SUBTITLES -> SubtitlePanel(player, state, settings, prefs, onPickSubtitle)
                                Panel.SPEED -> SpeedPanel(player, state.speed)
                                Panel.ASPECT -> AspectMode.entries.forEach { mode ->
                                    ChoiceRow(stringResource(mode.labelRes), aspect == mode) { onAspect(mode) }
                                }
                                Panel.SLEEP -> SleepPanel(onHint) { onPanel(Panel.NONE) }
                                Panel.REPEAT -> {
                                    ChoiceRow(stringResource(R.string.repeat_off), state.repeatMode == Player.REPEAT_MODE_OFF) {
                                        player.repeatMode = Player.REPEAT_MODE_OFF
                                    }
                                    ChoiceRow(stringResource(R.string.repeat_one), state.repeatMode == Player.REPEAT_MODE_ONE) {
                                        player.repeatMode = Player.REPEAT_MODE_ONE
                                    }
                                    ChoiceRow(stringResource(R.string.repeat_all), state.repeatMode == Player.REPEAT_MODE_ALL) {
                                        player.repeatMode = Player.REPEAT_MODE_ALL
                                    }
                                }
                                Panel.DECODER -> DecoderPanel(settings, prefs)
                                Panel.QUEUE -> QueuePanel(player, state)
                                Panel.INFO -> InfoPanel(player, state)
                                Panel.NONE -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Panel.titleRes(): Int = when (this) {
    Panel.MORE, Panel.NONE -> R.string.more
    Panel.AUDIO -> R.string.player_audio_track
    Panel.SUBTITLES -> R.string.player_subtitles
    Panel.SPEED -> R.string.player_speed
    Panel.ASPECT -> R.string.player_aspect
    Panel.SLEEP -> R.string.player_sleep_timer
    Panel.REPEAT -> R.string.player_repeat
    Panel.DECODER -> R.string.player_decoder
    Panel.QUEUE -> R.string.player_queue
    Panel.INFO -> R.string.player_info
}

@Composable
private fun PanelHeader(title: String, showBack: Boolean, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun MorePanel(
    player: ExoPlayer,
    state: PlayerUiState,
    settings: Settings,
    prefs: Prefs,
    abActive: Boolean,
    onPanel: (Panel) -> Unit,
    onAbRepeat: () -> Unit,
) {
    val sleep by PlayerHolder.sleep.collectAsState()
    MenuRow(Icons.Rounded.Speed, stringResource(R.string.player_speed), formatSpeed(state.speed)) { onPanel(Panel.SPEED) }
    if (!state.isAudioItem) {
        MenuRow(Icons.Rounded.AspectRatio, stringResource(R.string.player_aspect)) { onPanel(Panel.ASPECT) }
    }
    MenuRow(
        Icons.Rounded.Bedtime, stringResource(R.string.player_sleep_timer),
        when (val s = sleep) {
            SleepTimer.Off -> stringResource(R.string.sleep_off)
            SleepTimer.EndOfItem -> stringResource(R.string.sleep_end_of_video)
            is SleepTimer.Until -> stringResource(R.string.sleep_minutes, s.minutes)
        },
    ) { onPanel(Panel.SLEEP) }
    MenuRow(
        Icons.Rounded.Repeat, stringResource(R.string.player_repeat),
        stringResource(
            when (state.repeatMode) {
                Player.REPEAT_MODE_ONE -> R.string.repeat_one
                Player.REPEAT_MODE_ALL -> R.string.repeat_all
                else -> R.string.repeat_off
            }
        ),
    ) { onPanel(Panel.REPEAT) }
    SwitchRow(Icons.Rounded.Shuffle, stringResource(R.string.player_shuffle), state.shuffle) {
        player.shuffleModeEnabled = it
    }
    MenuRow(
        Icons.Rounded.SwapHoriz, stringResource(R.string.player_ab_repeat),
        if (abActive) "A–B" else null, onClick = onAbRepeat,
    )
    SwitchRow(Icons.Rounded.Headphones, stringResource(R.string.player_background_play), settings.backgroundPlay) { on ->
        prefs.update { it.copy(backgroundPlay = on) }
    }
    MenuRow(Icons.Rounded.Memory, stringResource(R.string.player_decoder), settings.decoder.shortLabel()) {
        onPanel(Panel.DECODER)
    }
    if (state.count > 1) {
        MenuRow(
            Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.player_queue),
            "${state.index + 1}/${state.count}",
        ) { onPanel(Panel.QUEUE) }
    }
    MenuRow(Icons.Rounded.Info, stringResource(R.string.player_info)) { onPanel(Panel.INFO) }
}

private fun DecoderMode.shortLabel(): String = when (this) {
    DecoderMode.HW -> "HW"
    DecoderMode.HW_PLUS -> "HW+"
    DecoderMode.SW -> "SW"
}

@Composable
private fun TrackPanel(player: ExoPlayer, tracks: Tracks, type: Int) {
    val context = LocalContext.current
    var number = 0
    var any = false
    tracks.groups.forEach { group ->
        if (group.type != type) return@forEach
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            number++
            any = true
            val label = trackLabel(group.getTrackFormat(i), context.getString(R.string.player_track_n, number), type)
            ChoiceRow(label, group.isTrackSelected(i)) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(type, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                    .build()
            }
        }
    }
    if (!any && type == C.TRACK_TYPE_AUDIO) {
        Text(
            stringResource(R.string.player_no_tracks),
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun trackLabel(format: Format, fallback: String, type: Int): String {
    val parts = ArrayList<String>()
    format.label?.takeIf { it.isNotBlank() }?.let { parts += it }
    format.language?.takeIf { it.isNotBlank() && it != "und" }?.let { tag ->
        val name = Locale.forLanguageTag(tag).getDisplayLanguage(Locale.getDefault())
        if (name.isNotBlank() && parts.none { it.equals(name, ignoreCase = true) }) parts += name
    }
    if (parts.isEmpty()) parts += fallback
    if (type == C.TRACK_TYPE_AUDIO) {
        when (format.channelCount) {
            1 -> parts += "Mono"
            2 -> parts += "Stereo"
            6 -> parts += "5.1"
            8 -> parts += "7.1"
        }
        codecName(format)?.let { parts += it }
    }
    return parts.joinToString(" · ")
}

private fun codecName(format: Format): String? {
    val mime = format.sampleMimeType ?: return format.codecs
    return when (val name = mime.substringAfter('/').lowercase(Locale.US)) {
        "avc" -> "H.264"
        "hevc" -> "H.265"
        "x-vnd.on2.vp9" -> "VP9"
        "x-vnd.on2.vp8" -> "VP8"
        "av01" -> "AV1"
        "mp4a-latm" -> "AAC"
        "ac3" -> "AC-3"
        "eac3" -> "E-AC-3"
        "vnd.dts" -> "DTS"
        "vnd.dts.hd" -> "DTS-HD"
        "true-hd" -> "TrueHD"
        "mpeg" -> if (mime.startsWith("audio")) "MP3" else "MPEG"
        "mp4v-es" -> "MPEG-4"
        "raw" -> "PCM"
        else -> name.uppercase(Locale.US)
    }
}

@Composable
private fun SubtitlePanel(
    player: ExoPlayer,
    state: PlayerUiState,
    settings: Settings,
    prefs: Prefs,
    onPickSubtitle: () -> Unit,
) {
    val hasSelectedText = state.tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
    ChoiceRow(stringResource(R.string.player_subtitles_off), state.textDisabled || !hasSelectedText) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }
    TrackPanel(player, state.tracks, C.TRACK_TYPE_TEXT)
    MenuRow(Icons.Rounded.FolderOpen, stringResource(R.string.player_subtitles_open), onClick = onPickSubtitle)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.player_subtitle_size), style = MaterialTheme.typography.titleSmall)
        Text(
            "${(settings.subtitleScale * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    Slider(
        value = settings.subtitleScale,
        onValueChange = { v -> prefs.update { it.copy(subtitleScale = (v * 20).roundToInt() / 20f) } },
        valueRange = 0.5f..2.5f,
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.secondary,
            activeTrackColor = MaterialTheme.colorScheme.secondary,
        ),
    )
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedPanel(player: ExoPlayer, speed: Float) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Text(
            formatSpeed(speed),
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Slider(
            value = speed,
            onValueChange = { player.setPlaybackSpeed((it * 20).roundToInt() / 20f) },
            valueRange = 0.25f..4f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
            ),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f).forEach { preset ->
                val selected = speed == preset
                Text(
                    if (preset == 1f) stringResource(R.string.player_speed_normal) else formatSpeed(preset),
                    Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .clickable(role = Role.RadioButton) { player.setPlaybackSpeed(preset) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SleepPanel(onHint: (String) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val sleep by PlayerHolder.sleep.collectAsState()
    ChoiceRow(stringResource(R.string.sleep_off), sleep is SleepTimer.Off) {
        PlayerHolder.setSleep(SleepTimer.Off)
        onHint(context.getString(R.string.sleep_cleared))
        onDone()
    }
    listOf(15, 30, 45, 60, 90).forEach { minutes ->
        ChoiceRow(
            stringResource(R.string.sleep_minutes, minutes),
            (sleep as? SleepTimer.Until)?.minutes == minutes,
        ) {
            PlayerHolder.setSleep(SleepTimer.Until(System.currentTimeMillis() + minutes * 60_000L, minutes))
            onHint(context.getString(R.string.sleep_set, minutes))
            onDone()
        }
    }
    ChoiceRow(stringResource(R.string.sleep_end_of_video), sleep is SleepTimer.EndOfItem) {
        PlayerHolder.setSleep(SleepTimer.EndOfItem)
        onHint(context.getString(R.string.sleep_set_end))
        onDone()
    }
}

@Composable
private fun DecoderPanel(settings: Settings, prefs: Prefs) {
    val context = LocalContext.current
    listOf(
        DecoderMode.HW to R.string.decoder_hw,
        DecoderMode.HW_PLUS to R.string.decoder_hw_plus,
        DecoderMode.SW to R.string.decoder_sw,
    ).forEach { (mode, label) ->
        ChoiceRow(stringResource(label), settings.decoder == mode) {
            if (settings.decoder != mode) {
                prefs.update { it.copy(decoder = mode) }
                PlayerHolder.rebuild(context)
            }
        }
    }
}

@Composable
private fun QueuePanel(player: ExoPlayer, state: PlayerUiState) {
    for (i in 0 until state.count) {
        val title = player.getMediaItemAt(i).mediaMetadata.title?.toString().orEmpty()
        ChoiceRow(title, i == state.index) {
            if (i != player.currentMediaItemIndex) {
                player.seekToDefaultPosition(i)
                player.play()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun InfoPanel(player: ExoPlayer, state: PlayerUiState) {
    val video = player.videoFormat
    val audio = player.audioFormat
    InfoRow(stringResource(R.string.info_name), state.title)
    InfoRow(stringResource(R.string.info_location), state.mediaId)
    if (state.durationMs > 0) InfoRow(stringResource(R.string.info_duration), formatDuration(state.durationMs))
    if (video != null) {
        if (video.width > 0) InfoRow(stringResource(R.string.info_resolution), "${video.width} × ${video.height}")
        codecName(video)?.let { InfoRow(stringResource(R.string.info_video_codec), it) }
        if (video.frameRate > 0) {
            InfoRow(stringResource(R.string.info_frame_rate), String.format(Locale.US, "%.2f fps", video.frameRate))
        }
        if (video.bitrate > 0) {
            InfoRow(stringResource(R.string.info_bitrate), String.format(Locale.US, "%.1f Mbps", video.bitrate / 1_000_000f))
        }
    }
    if (audio != null) {
        val details = listOfNotNull(
            codecName(audio),
            audio.sampleRate.takeIf { it > 0 }?.let { "$it Hz" },
            audio.channelCount.takeIf { it > 0 }?.let { "$it ch" },
        ).joinToString(" · ")
        if (details.isNotEmpty()) InfoRow(stringResource(R.string.info_audio_codec), details)
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        }
    }
}
