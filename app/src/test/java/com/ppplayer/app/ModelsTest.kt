package com.ppplayer.app

import com.ppplayer.app.data.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    @Test
    fun historyProgress() {
        assertEquals(0.5f, HistoryEntry("u", "t", 50, 100, false, 0).progress, 0.0001f)
        assertEquals(0f, HistoryEntry("u", "t", 50, 0, false, 0).progress, 0.0001f)
        assertEquals(1f, HistoryEntry("u", "t", 500, 100, false, 0).progress, 0.0001f)
    }
}
