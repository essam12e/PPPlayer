package com.ppplayer.app.player

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings as SystemSettings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ppplayer.app.PPPlayerApp
import com.ppplayer.app.data.OrientationMode
import com.ppplayer.app.data.ThemeMode
import com.ppplayer.app.ui.theme.PPPlayerTheme
import com.ppplayer.app.util.subtitleMimeType

/** What the player UI can ask of the hosting activity. */
interface PlayerHost {
    fun exit()
    fun brightness(): Float
    fun setBrightness(value: Float)
    fun toggleOrientation()
    fun applyOrientation(videoWidth: Int, videoHeight: Int, isAudio: Boolean)
    fun enterPip()
    val canPip: Boolean
    fun pickSubtitle()
    fun keepScreenOn(on: Boolean)
}

class PlayerActivity : ComponentActivity(), PlayerHost {

    private val prefs get() = (application as PPPlayerApp).prefs
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val inPip = mutableStateOf(false)
    private val subtitleMessage = mutableStateOf(0)
    private var resumeAfterPicker = false

    private val subtitlePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) attachSubtitle(uri)
        if (resumeAfterPicker) PlayerHolder.player.value?.play()
        resumeAfterPicker = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PlayerHolder.activityAttached = true
        volumeControlStream = AudioManager.STREAM_MUSIC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()

        if (savedInstanceState == null) {
            handleIntent(intent)
        } else if ((PlayerHolder.player.value?.mediaItemCount ?: 0) == 0) {
            finish()
            return
        }

        setContent {
            val player by PlayerHolder.player.collectAsState()
            val settings by prefs.settings.collectAsState()
            PPPlayerTheme(mode = ThemeMode.DARK) {
                player?.let { p ->
                    PlayerScreen(
                        player = p,
                        settings = settings,
                        prefs = prefs,
                        inPip = inPip.value,
                        subtitleLoadedSignal = subtitleMessage.value,
                        host = this,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            val isAudio = (intent.type ?: contentResolver.getType(data))?.startsWith("audio/") == true
            PlayerHolder.play(this, listOf(QueueItem(data.toString(), displayName(data), isAudio)), 0)
            return
        }
        val queue = pendingQueue
        pendingQueue = null
        if (queue != null) {
            PlayerHolder.play(this, queue, pendingIndex)
        } else if ((PlayerHolder.player.value?.mediaItemCount ?: 0) == 0) {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (controllerFuture == null) {
            // Binding a controller starts the playback service, which owns the media notification.
            val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(this, token).buildAsync()
        }
    }

    override fun onStop() {
        super.onStop()
        val player = PlayerHolder.player.value ?: return
        PlayerHolder.saveCurrent(player)
        if (!isFinishing && !keepsPlayingInBackground()) {
            player.pause()
        }
    }

    override fun onDestroy() {
        PlayerHolder.activityAttached = false
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    private fun keepsPlayingInBackground(): Boolean {
        val player = PlayerHolder.player.value ?: return false
        return prefs.settings.value.backgroundPlay || player.currentMediaItem?.isAudio == true
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0)?.let { return it.substringBeforeLast('.', it) }
                }
            }
        }
        val last = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        return last?.substringBeforeLast('.', last) ?: uri.toString()
    }

    private fun fileName(uri: Uri): String {
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { return it }
            }
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun attachSubtitle(uri: Uri) {
        val player = PlayerHolder.player.value ?: return
        val current = player.currentMediaItem ?: return
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = fileName(uri)
        val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(subtitleMimeType(name) ?: "application/x-subrip")
            .setLabel(name.substringBeforeLast('.', name))
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        player.replaceMediaItem(index, current.buildUpon().setSubtitleConfigurations(listOf(subtitle)).build())
        player.seekTo(index, position)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(null)
            .build()
        player.prepare()
        subtitleMessage.value++
    }

    // ---- PlayerHost --------------------------------------------------------------------------

    override fun exit() {
        val player = PlayerHolder.player.value
        val keep = player != null && player.isPlaying && keepsPlayingInBackground()
        if (!keep) PlayerHolder.release()
        finish()
    }

    override fun brightness(): Float {
        val current = window.attributes.screenBrightness
        if (current >= 0f) return current
        return runCatching {
            SystemSettings.System.getInt(contentResolver, SystemSettings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f).coerceIn(0.01f, 1f)
    }

    override fun setBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value.coerceIn(0.01f, 1f) }
    }

    override fun toggleOrientation() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun applyOrientation(videoWidth: Int, videoHeight: Int, isAudio: Boolean) {
        requestedOrientation = when {
            isAudio -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            prefs.settings.value.orientation == OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            prefs.settings.value.orientation == OrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            videoWidth <= 0 || videoHeight <= 0 -> return
            videoWidth >= videoHeight -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    override val canPip: Boolean
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    override fun enterPip() {
        if (!canPip) return
        val size = PlayerHolder.player.value?.videoSize
        val builder = PictureInPictureParams.Builder()
        if (size != null && size.width > 0 && size.height > 0) {
            val ratio = (size.width.toFloat() / size.height).coerceIn(0.42f, 2.38f)
            builder.setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        }
        runCatching { enterPictureInPictureMode(builder.build()) }
    }

    override fun pickSubtitle() {
        resumeAfterPicker = PlayerHolder.player.value?.isPlaying == true
        runCatching { subtitlePicker.launch(arrayOf("*/*")) }.onFailure { resumeAfterPicker = false }
    }

    override fun keepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private var pendingQueue: List<QueueItem>? = null
        private var pendingIndex = 0

        /** Opens the player with a fresh queue. */
        fun start(context: Context, items: List<QueueItem>, index: Int) {
            if (items.isEmpty()) return
            pendingQueue = items
            pendingIndex = index
            context.startActivity(Intent(context, PlayerActivity::class.java))
        }

        /** Returns to whatever is already playing. */
        fun resume(context: Context) {
            context.startActivity(Intent(context, PlayerActivity::class.java))
        }
    }
}
