package dev.sonora.core.domain

/** Formats durations for track rows, the player scrubber and preview badges. */
object DurationFormat {

    private const val UNKNOWN = "--:--"

    /** `3:07`, `1:02:11`, or `--:--` when the value is missing. */
    fun format(millis: Long?): String {
        if (millis == null || millis < 0) return UNKNOWN
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** Compact label used by the "Preview • 0:30" badge. */
    fun previewLabel(millis: Long?): String {
        val formatted = format(millis ?: 30_000L)
        return if (formatted == UNKNOWN) "0:30" else formatted
    }

    /** Release year from an ISO-8601 timestamp such as `2024-05-31T07:00:00Z`. */
    fun year(releaseDate: String?): String? {
        val date = releaseDate?.trim().orEmpty()
        if (date.length < 4) return null
        val year = date.take(4)
        return year.takeIf { it.all(Char::isDigit) }
    }
}
