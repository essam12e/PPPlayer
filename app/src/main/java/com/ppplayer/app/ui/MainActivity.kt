package com.ppplayer.app.ui

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings as SystemSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import com.ppplayer.app.PPPlayerApp
import com.ppplayer.app.R
import com.ppplayer.app.data.MediaFile
import com.ppplayer.app.data.MediaRepository
import com.ppplayer.app.data.ThemeMode
import com.ppplayer.app.ui.home.HomeScreen
import com.ppplayer.app.ui.home.LibraryViewModel
import com.ppplayer.app.ui.home.MediaActions
import com.ppplayer.app.ui.theme.PPPlayerTheme

class MainActivity : ComponentActivity(), MediaActions {

    private val prefs get() = (application as PPPlayerApp).prefs
    private val library: LibraryViewModel by viewModels()
    private var afterGrant: (() -> Unit)? = null
    private var askedOnce = false

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            askedOnce = true
            library.refresh()
        }

    private val grantRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val action = afterGrant
            afterGrant = null
            if (result.resultCode == Activity.RESULT_OK) action?.invoke()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by prefs.settings.collectAsState()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            PPPlayerTheme(mode = settings.theme) {
                HomeScreen(library = library, prefs = prefs, actions = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        library.refresh()
    }

    // ---- MediaActions ------------------------------------------------------------------------

    override fun requestLibraryAccess() {
        val permissions = buildList {
            addAll(MediaRepository.requiredPermissions())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        val blocked = askedOnce && MediaRepository.requiredPermissions().none {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
        if (blocked) openAppSettings() else permissionRequest.launch(permissions)
    }

    override fun openAppSettings() {
        startActivity(
            Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    override fun share(file: MediaFile) {
        val send = Intent(Intent.ACTION_SEND)
            .setType(if (file.isAudio) "audio/*" else "video/*")
            .putExtra(Intent.EXTRA_STREAM, file.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivity(Intent.createChooser(send, getString(R.string.action_share))) }
    }

    override val canRename: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun delete(file: MediaFile) {
        val done = {
            prefs.removeFromHistory(file.uri.toString())
            toast(R.string.deleted)
            library.refresh()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            afterGrant = done
            launchGrant { MediaStore.createDeleteRequest(contentResolver, listOf(file.uri)).intentSender }
            return
        }
        fun attempt() {
            try {
                if (contentResolver.delete(file.uri, null, null) > 0) done() else toast(R.string.operation_failed)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    afterGrant = { attempt() }
                    launchGrant { e.userAction.actionIntent.intentSender }
                } else toast(R.string.operation_failed)
            }
        }
        attempt()
    }

    override fun rename(file: MediaFile, newTitle: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val extension = file.name.substringAfterLast('.', "")
        val clean = newTitle.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (clean.isEmpty()) return
        val newName = if (extension.isEmpty()) clean else "$clean.$extension"
        afterGrant = {
            val ok = runCatching {
                contentResolver.update(
                    file.uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) },
                    null,
                    null,
                ) > 0
            }.getOrDefault(false)
            toast(if (ok) R.string.renamed else R.string.operation_failed)
            library.refresh()
        }
        launchGrant { MediaStore.createWriteRequest(contentResolver, listOf(file.uri)).intentSender }
    }

    private fun launchGrant(sender: () -> android.content.IntentSender) {
        runCatching { grantRequest.launch(IntentSenderRequest.Builder(sender()).build()) }
            .onFailure {
                afterGrant = null
                toast(R.string.operation_failed)
            }
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
}
