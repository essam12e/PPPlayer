package com.ppplayer.app.util

import java.util.Locale

/** Formats a duration as m:ss or h:mm:ss. Digits stay Latin so times read the same in every language. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** Signed variant used by the seek gesture: +0:15 / -1:02. */
fun formatSignedDuration(ms: Long): String =
    (if (ms < 0) "-" else "+") + formatDuration(kotlin.math.abs(ms))

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0 || value >= 100) String.format(Locale.US, "%.0f %s", value, units[unit])
    else String.format(Locale.US, "%.1f %s", value, units[unit])
}

fun formatSpeed(speed: Float): String {
    val text = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    return "$text×"
}

/** Maps a subtitle file name to the MIME type the player expects, or null when unsupported. */
fun subtitleMimeType(fileName: String): String? =
    when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
        "srt" -> "application/x-subrip"
        "vtt", "webvtt" -> "text/vtt"
        "ass", "ssa" -> "text/x-ssa"
        "ttml", "xml", "dfxp" -> "application/ttml+xml"
        else -> null
    }

/** Accepts the link forms the network stream dialog allows and returns a cleaned URL, or null. */
fun normalizeStreamUrl(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val lower = text.lowercase(Locale.US)
    val schemes = listOf("http://", "https://", "rtsp://")
    val scheme = schemes.firstOrNull { lower.startsWith(it) } ?: return null
    val rest = text.substring(scheme.length)
    if (rest.isEmpty() || rest.startsWith("/")) return null
    return text
}

/** Keeps a playback position only when it is worth resuming from. */
fun resumablePosition(positionMs: Long, durationMs: Long): Long {
    if (positionMs < 5_000) return 0
    if (durationMs > 0 && positionMs > durationMs - 5_000) return 0
    return positionMs
}
