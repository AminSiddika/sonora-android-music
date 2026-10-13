package dev.sonora.core.domain

/**
 * iTunes returns artwork with a size token embedded in the file name, e.g.
 * `.../100x100bb.jpg`. The same image is available at other sizes by swapping
 * that token, which keeps the UI crisp without extra requests.
 */
object ArtworkUrls {

    /** Size used for detail screens and the full player. */
    const val LARGE_SIZE: Int = 600

    /** Size used for list rows and the mini player. */
    const val MEDIUM_SIZE: Int = 200

    private val sizeToken = Regex("""/(\d+)x(\d+)(bb|ff|ss)\.(jpg|jpeg|png|webp)""", RegexOption.IGNORE_CASE)

    fun upgrade(url: String?, size: Int = LARGE_SIZE): String? {
        val clean = url?.trim().orEmpty()
        if (clean.isEmpty()) return null
        if (size <= 0) return clean
        val replaced = sizeToken.replace(clean) { match ->
            val suffix = match.groupValues[3]
            val extension = match.groupValues[4]
            "/${size}x$size$suffix.$extension"
        }
        return replaced
    }
}
