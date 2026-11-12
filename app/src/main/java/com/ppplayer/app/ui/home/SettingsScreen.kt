package com.ppplayer.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ppplayer.app.R
import com.ppplayer.app.data.DecoderMode
import com.ppplayer.app.data.OrientationMode
import com.ppplayer.app.data.Prefs
import com.ppplayer.app.data.Settings
import com.ppplayer.app.data.ThemeMode
import com.ppplayer.app.player.PlayerHolder
import com.ppplayer.app.ui.theme.TABULAR
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: Settings, prefs: Prefs) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        TabHeader(stringResource(R.string.tab_settings))

        Section(stringResource(R.string.settings_appearance)) {
            ChoiceSetting(
                title = stringResource(R.string.settings_theme),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                ),
                selected = settings.theme,
            ) { value -> prefs.update { it.copy(theme = value) } }
        }

        Section(stringResource(R.string.settings_playback)) {
            SwitchSetting(
                stringResource(R.string.settings_resume), stringResource(R.string.settings_resume_desc), settings.resume,
            ) { on -> prefs.update { it.copy(resume = on) } }
            SwitchSetting(
                stringResource(R.string.settings_autoplay), stringResource(R.string.settings_autoplay_desc), settings.autoPlayNext,
            ) { on ->
                prefs.update { it.copy(autoPlayNext = on) }
                PlayerHolder.applySettings()
            }
            SwitchSetting(
                stringResource(R.string.settings_background), stringResource(R.string.settings_background_desc), settings.backgroundPlay,
            ) { on -> prefs.update { it.copy(backgroundPlay = on) } }
            ChoiceSetting(
                title = stringResource(R.string.settings_orientation),
                options = listOf(
                    OrientationMode.VIDEO to stringResource(R.string.orientation_video),
                    OrientationMode.LANDSCAPE to stringResource(R.string.orientation_landscape),
                    OrientationMode.SENSOR to stringResource(R.string.orientation_sensor),
                ),
                selected = settings.orientation,
            ) { value -> prefs.update { it.copy(orientation = value) } }
            ChoiceSetting(
                title = stringResource(R.string.settings_seek_step),
                options = listOf(5, 10, 15, 20, 30).map { it to stringResource(R.string.seconds_value, it) },
                selected = settings.seekStepSec,
            ) { value ->
                prefs.update { it.copy(seekStepSec = value) }
                PlayerHolder.rebuild(context)
            }
            ChoiceSetting(
                title = stringResource(R.string.settings_decoder),
                options = listOf(
                    DecoderMode.HW to stringResource(R.string.decoder_hw),
                    DecoderMode.HW_PLUS to stringResource(R.string.decoder_hw_plus),
                    DecoderMode.SW to stringResource(R.string.decoder_sw),
                ),
                selected = settings.decoder,
            ) { value ->
                prefs.update { it.copy(decoder = value) }
                PlayerHolder.rebuild(context)
            }
        }

        Section(stringResource(R.string.settings_gestures)) {
            SwitchSetting(
                stringResource(R.string.settings_gesture_brightness), stringResource(R.string.settings_gesture_brightness_desc),
                settings.gestureBrightness,
            ) { on -> prefs.update { it.copy(gestureBrightness = on) } }
            SwitchSetting(
                stringResource(R.string.settings_gesture_volume), stringResource(R.string.settings_gesture_volume_desc),
                settings.gestureVolume,
            ) { on -> prefs.update { it.copy(gestureVolume = on) } }
            SwitchSetting(
                stringResource(R.string.settings_gesture_seek), stringResource(R.string.settings_gesture_seek_desc),
                settings.gestureSeek,
            ) { on -> prefs.update { it.copy(gestureSeek = on) } }
            SwitchSetting(
                stringResource(R.string.settings_gesture_double_tap), stringResource(R.string.settings_gesture_double_tap_desc),
                settings.gestureDoubleTap,
            ) { on -> prefs.update { it.copy(gestureDoubleTap = on) } }
            SwitchSetting(
                stringResource(R.string.settings_gesture_zoom), stringResource(R.string.settings_gesture_zoom_desc),
                settings.gestureZoom,
            ) { on -> prefs.update { it.copy(gestureZoom = on) } }
            SwitchSetting(
                stringResource(R.string.settings_gesture_speed), stringResource(R.string.settings_gesture_speed_desc),
                settings.gestureHoldSpeed,
            ) { on -> prefs.update { it.copy(gestureHoldSpeed = on) } }
        }

        Section(stringResource(R.string.settings_subtitles)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.player_subtitle_size), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${(settings.subtitleScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = settings.subtitleScale,
                onValueChange = { v -> prefs.update { it.copy(subtitleScale = (v * 20).roundToInt() / 20f) } },
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Section(stringResource(R.string.settings_about)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.brand_logo), null,
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    val version = remember {
                        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                    }
                    Text(
                        stringResource(R.string.settings_version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 6.dp)
    ) { content() }
}

@Composable
private fun SwitchSetting(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> ChoiceSetting(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                options.firstOrNull { it.first == selected }?.second.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.RadioButton) {
                                open = false
                                if (value != selected) onSelect(value)
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (value == selected) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
    )
}
