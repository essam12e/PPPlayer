package com.ppplayer.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class OrientationMode { VIDEO, LANDSCAPE, SENSOR }
enum class DecoderMode { HW, HW_PLUS, SW }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val resume: Boolean = true,
    val autoPlayNext: Boolean = true,
    val backgroundPlay: Boolean = false,
    val orientation: OrientationMode = OrientationMode.VIDEO,
    val seekStepSec: Int = 10,
    val decoder: DecoderMode = DecoderMode.HW_PLUS,
    val gestureBrightness: Boolean = true,
    val gestureVolume: Boolean = true,
    val gestureSeek: Boolean = true,
    val gestureDoubleTap: Boolean = true,
    val gestureZoom: Boolean = true,
    val gestureHoldSpeed: Boolean = true,
    val subtitleScale: Float = 1f,
    val gridView: Boolean = false,
    val showFolders: Boolean = true,
    val sortField: SortField = SortField.DATE,
    val sortDescending: Boolean = true,
)

/** App settings, saved playback positions and the Recent list, all backed by SharedPreferences. */
class Prefs(context: Context) {

    private val sp: SharedPreferences = context.getSharedPreferences("ppplayer", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        _settings.update(transform)
        save(_settings.value)
    }

    /** Last saved position for [uri] in milliseconds, or 0. */
    fun positionOf(uri: String): Long =
        _history.value.firstOrNull { it.uri == uri }?.positionMs ?: 0L

    fun record(entry: HistoryEntry) {
        _history.update { current ->
            (listOf(entry) + current.filterNot { it.uri == entry.uri }).take(MAX_HISTORY)
        }
        saveHistory()
    }

    fun removeFromHistory(uri: String) {
        _history.update { list -> list.filterNot { it.uri == uri } }
        saveHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            theme = enumOf(sp.getString("theme", null), d.theme),
            resume = sp.getBoolean("resume", d.resume),
            autoPlayNext = sp.getBoolean("autoPlayNext", d.autoPlayNext),
            backgroundPlay = sp.getBoolean("backgroundPlay", d.backgroundPlay),
            orientation = enumOf(sp.getString("orientation", null), d.orientation),
            seekStepSec = sp.getInt("seekStepSec", d.seekStepSec),
            decoder = enumOf(sp.getString("decoder", null), d.decoder),
            gestureBrightness = sp.getBoolean("gestureBrightness", d.gestureBrightness),
            gestureVolume = sp.getBoolean("gestureVolume", d.gestureVolume),
            gestureSeek = sp.getBoolean("gestureSeek", d.gestureSeek),
            gestureDoubleTap = sp.getBoolean("gestureDoubleTap", d.gestureDoubleTap),
            gestureZoom = sp.getBoolean("gestureZoom", d.gestureZoom),
            gestureHoldSpeed = sp.getBoolean("gestureHoldSpeed", d.gestureHoldSpeed),
            subtitleScale = sp.getFloat("subtitleScale", d.subtitleScale),
            gridView = sp.getBoolean("gridView", d.gridView),
            showFolders = sp.getBoolean("showFolders", d.showFolders),
            sortField = enumOf(sp.getString("sortField", null), d.sortField),
            sortDescending = sp.getBoolean("sortDescending", d.sortDescending),
        )
    }

    private fun save(s: Settings) {
        sp.edit()
            .putString("theme", s.theme.name)
            .putBoolean("resume", s.resume)
            .putBoolean("autoPlayNext", s.autoPlayNext)
            .putBoolean("backgroundPlay", s.backgroundPlay)
            .putString("orientation", s.orientation.name)
            .putInt("seekStepSec", s.seekStepSec)
            .putString("decoder", s.decoder.name)
            .putBoolean("gestureBrightness", s.gestureBrightness)
            .putBoolean("gestureVolume", s.gestureVolume)
            .putBoolean("gestureSeek", s.gestureSeek)
            .putBoolean("gestureDoubleTap", s.gestureDoubleTap)
            .putBoolean("gestureZoom", s.gestureZoom)
            .putBoolean("gestureHoldSpeed", s.gestureHoldSpeed)
            .putFloat("subtitleScale", s.subtitleScale)
            .putBoolean("gridView", s.gridView)
            .putBoolean("showFolders", s.showFolders)
            .putString("sortField", s.sortField.name)
            .putBoolean("sortDescending", s.sortDescending)
            .apply()
    }

    private fun loadHistory(): List<HistoryEntry> = runCatching {
        val array = JSONArray(sp.getString("history", "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            HistoryEntry(
                uri = o.getString("uri"),
                title = o.optString("title"),
                positionMs = o.optLong("pos"),
                durationMs = o.optLong("dur"),
                isAudio = o.optBoolean("audio"),
                playedAt = o.optLong("at"),
            )
        }
    }.getOrDefault(emptyList())

    private fun saveHistory() {
        val array = JSONArray()
        _history.value.forEach { e ->
            array.put(
                JSONObject()
                    .put("uri", e.uri).put("title", e.title).put("pos", e.positionMs)
                    .put("dur", e.durationMs).put("audio", e.isAudio).put("at", e.playedAt)
            )
        }
        sp.edit().putString("history", array.toString()).apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val MAX_HISTORY = 100
    }
}
