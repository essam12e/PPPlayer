package com.ppplayer.app.data

import android.net.Uri

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val bucketId: Long,
    val bucketName: String,
    val width: Int,
    val height: Int,
    val path: String,
    val isAudio: Boolean,
    val artist: String?,
) {
    val title: String get() = name.substringBeforeLast('.', name)
    val resolutionLabel: String?
        get() {
            val shortSide = minOf(width, height)
            return when {
                shortSide <= 0 -> null
                shortSide >= 2160 -> "4K"
                shortSide >= 1440 -> "2K"
                shortSide >= 1080 -> "1080p"
                shortSide >= 720 -> "720p"
                shortSide >= 480 -> "480p"
                else -> "${shortSide}p"
            }
        }
}

data class MediaFolder(
    val id: Long,
    val name: String,
    val files: List<MediaFile>,
) {
    val totalSize: Long get() = files.sumOf { it.sizeBytes }
}

enum class SortField { NAME, DATE, SIZE, DURATION }

fun List<MediaFile>.sortedBy(field: SortField, descending: Boolean): List<MediaFile> {
    val comparator: Comparator<MediaFile> = when (field) {
        SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        SortField.DATE -> compareBy { it.dateAddedSec }
        SortField.SIZE -> compareBy { it.sizeBytes }
        SortField.DURATION -> compareBy { it.durationMs }
    }
    return if (descending) sortedWith(comparator.reversed()) else sortedWith(comparator)
}

fun List<MediaFile>.groupIntoFolders(): List<MediaFolder> =
    groupBy { it.bucketId }
        .map { (id, files) -> MediaFolder(id, files.first().bucketName, files) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<MediaFile>.matching(query: String): List<MediaFile> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { it.name.contains(q, ignoreCase = true) || (it.artist?.contains(q, ignoreCase = true) == true) }
}

/** One entry of the Recent list. */
data class HistoryEntry(
    val uri: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val isAudio: Boolean,
    val playedAt: Long,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
