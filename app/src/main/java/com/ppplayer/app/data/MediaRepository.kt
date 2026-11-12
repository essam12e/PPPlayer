package com.ppplayer.app.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the device's video and audio files from MediaStore. */
class MediaRepository(private val context: Context) {

    suspend fun loadVideos(): List<MediaFile> = withContext(Dispatchers.IO) { query(audio = false) }

    suspend fun loadAudio(): List<MediaFile> = withContext(Dispatchers.IO) { query(audio = true) }

    private fun query(audio: Boolean): List<MediaFile> {
        val collection =
            if (audio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATA)
            add(MediaStore.MediaColumns.DURATION)
            if (audio) {
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.IS_MUSIC)
            } else {
                add(MediaStore.Video.Media.BUCKET_ID)
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                add(MediaStore.MediaColumns.WIDTH)
                add(MediaStore.MediaColumns.HEIGHT)
            }
        }.toTypedArray()

        val result = ArrayList<MediaFile>()
        runCatching {
            context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val dataCol = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                val durCol = c.getColumnIndex(MediaStore.MediaColumns.DURATION)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val musicCol = c.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                val bucketIdCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val widthCol = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

                while (c.moveToNext()) {
                    if (audio && musicCol >= 0 && c.getInt(musicCol) == 0 && c.getLong(durCol.coerceAtLeast(0)) < 30_000) {
                        continue // skip ringtones and notification sounds
                    }
                    val id = c.getLong(idCol)
                    val path = if (dataCol >= 0) c.getString(dataCol).orEmpty() else ""
                    val name = (if (nameCol >= 0) c.getString(nameCol) else null)
                        ?: path.substringAfterLast('/').ifEmpty { id.toString() }
                    val artist = if (artistCol >= 0) c.getString(artistCol) else null
                    result += MediaFile(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        name = name,
                        durationMs = if (durCol >= 0) c.getLong(durCol) else 0L,
                        sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol) else 0L,
                        dateAddedSec = if (dateCol >= 0) c.getLong(dateCol) else 0L,
                        bucketId = if (bucketIdCol >= 0) c.getLong(bucketIdCol) else 0L,
                        bucketName = (if (bucketNameCol >= 0) c.getString(bucketNameCol) else null)
                            ?: path.substringBeforeLast('/', "").substringAfterLast('/').ifEmpty { "/" },
                        width = if (widthCol >= 0) c.getInt(widthCol) else 0,
                        height = if (heightCol >= 0) c.getInt(heightCol) else 0,
                        path = path,
                        isAudio = audio,
                        artist = artist?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING },
                    )
                }
            }
        }
        return result
    }

    companion object {
        /** The runtime permissions the library needs on this Android version. */
        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        /** True when at least the video part of the library can be read. */
        fun hasPermission(context: Context): Boolean {
            fun granted(p: String) =
                ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    granted(Manifest.permission.READ_MEDIA_VIDEO) ||
                        granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> granted(Manifest.permission.READ_MEDIA_VIDEO)
                else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}
