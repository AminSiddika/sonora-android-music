package dev.sonora.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DurationFormatTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("3:06", DurationFormat.format(186_000))
        assertEquals("0:30", DurationFormat.format(30_000))
        assertEquals("0:07", DurationFormat.format(7_400))
    }

    @Test
    fun `formats long durations with hours`() {
        assertEquals("1:02:11", DurationFormat.format(3_731_000))
    }

    @Test
    fun `uses a placeholder for unknown durations`() {
        assertEquals("--:--", DurationFormat.format(null))
        assertEquals("--:--", DurationFormat.format(-1))
    }

    @Test
    fun `preview label always shows a value`() {
        assertEquals("0:30", DurationFormat.previewLabel(null))
        assertEquals("0:30", DurationFormat.previewLabel(30_000))
    }

    @Test
    fun `extracts the release year`() {
        assertEquals("2002", DurationFormat.year("2002-02-26T08:00:00Z"))
        assertNull(DurationFormat.year(null))
        assertNull(DurationFormat.year("not-a-date"))
        assertNull(DurationFormat.year("20"))
    }
}
