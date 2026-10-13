package dev.sonora.core.domain

/** Outcome of validating raw user input from the search field. */
sealed interface QueryValidation {
    data class Valid(val query: String) : QueryValidation

    data object Blank : QueryValidation

    data class TooLong(val maxLength: Int) : QueryValidation
}

/**
 * Normalises and validates search terms. iTunes and YouTube both accept free
 * text, so the only real rules are "not empty" and "not absurdly long".
 */
object SearchQueryValidator {

    const val MAX_LENGTH: Int = 120

    fun normalize(raw: String): String = raw
        .filterNot { it.isControlCharacter() }
        .trim()
        .replace(Regex("\\s+"), " ")

    fun validate(raw: String): QueryValidation {
        val normalized = normalize(raw)
        return when {
            normalized.isEmpty() -> QueryValidation.Blank
            normalized.length > MAX_LENGTH -> QueryValidation.TooLong(MAX_LENGTH)
            else -> QueryValidation.Valid(normalized)
        }
    }

    private fun Char.isControlCharacter(): Boolean = this.code < 0x20 && this != '\t' && this != '\n'
}
