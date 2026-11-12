package com.ppplayer.app

import com.ppplayer.app.util.formatDuration
import com.ppplayer.app.util.formatSignedDuration
import com.ppplayer.app.util.formatSize
import com.ppplayer.app.util.formatSpeed
import com.ppplayer.app.util.normalizeStreamUrl
import com.ppplayer.app.util.resumablePosition
import com.ppplayer.app.util.subtitleMimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {

    @Test
    fun durations() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5))
        assertEquals("0:59", formatDuration(59_999))
        assertEquals("1:00", formatDuration(60_000))
        assertEquals("1:01:01", formatDuration(3_661_000))
        assertEquals("+0:15", formatSignedDuration(15_000))
        assertEquals("-1:02", formatSignedDuration(-62_000))
    }

    @Test
    fun sizes() {
        assertEquals("0 B", formatSize(0))
        assertEquals("512 B", formatSize(512))
        assertEquals("1.0 KB", formatSize(1024))
        assertEquals("1.5 MB", formatSize(1024L * 1024 * 3 / 2))
        assertEquals("2.0 GB", formatSize(2L * 1024 * 1024 * 1024))
        assertEquals("150 MB", formatSize(150L * 1024 * 1024))
    }

    @Test
    fun speeds() {
        assertEquals("1×", formatSpeed(1f))
        assertEquals("1.25×", formatSpeed(1.25f))
        assertEquals("0.5×", formatSpeed(0.5f))
        assertEquals("2×", formatSpeed(2f))
    }

    @Test
    fun subtitleTypes() {
        assertEquals("application/x-subrip", subtitleMimeType("Movie.EN.SRT"))
        assertEquals("text/vtt", subtitleMimeType("a.vtt"))
        assertEquals("text/x-ssa", subtitleMimeType("a.ass"))
        assertEquals("application/ttml+xml", subtitleMimeType("a.ttml"))
        assertNull(subtitleMimeType("a.mp4"))
        assertNull(subtitleMimeType("noextension"))
    }

    @Test
    fun streamUrls() {
        assertEquals("https://example.com/a.m3u8", normalizeStreamUrl("  https://example.com/a.m3u8 "))
        assertEquals("RTSP://cam.local/live", normalizeStreamUrl("RTSP://cam.local/live"))
        assertNull(normalizeStreamUrl(""))
        assertNull(normalizeStreamUrl("example.com/video.mp4"))
        assertNull(normalizeStreamUrl("ftp://example.com/a.mp4"))
        assertNull(normalizeStreamUrl("https://"))
        assertNull(normalizeStreamUrl("https:///path"))
        assertNull(normalizeStreamUrl("https://exa mple.com/a"))
    }

    @Test
    fun resumePositions() {
        assertEquals(0, resumablePosition(4_000, 600_000))        // barely started
        assertEquals(120_000, resumablePosition(120_000, 600_000))
        assertEquals(0, resumablePosition(597_000, 600_000))      // essentially finished
        assertEquals(30_000, resumablePosition(30_000, 0))        // unknown duration
    }
}
