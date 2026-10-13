package dev.sonora.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SearchQueryValidatorTest {

    @Test
    fun `collapses whitespace and trims`() {
        assertEquals("norah jones", SearchQueryValidator.normalize("  norah    jones  "))
    }

    @Test
    fun `keeps accented and non latin characters`() {
        val validated = SearchQueryValidator.validate("Björk – Jóga")
        assertIs<QueryValidation.Valid>(validated)
        assertEquals("Björk – Jóga", validated.query)
    }

    @Test
    fun `rejects blank input`() {
        assertIs<QueryValidation.Blank>(SearchQueryValidator.validate("   "))
        assertIs<QueryValidation.Blank>(SearchQueryValidator.validate(""))
    }

    @Test
    fun `rejects overly long input`() {
        val long = "a".repeat(SearchQueryValidator.MAX_LENGTH + 1)
        assertIs<QueryValidation.TooLong>(SearchQueryValidator.validate(long))
    }

    @Test
    fun `accepts input at the length limit`() {
        val atLimit = "a".repeat(SearchQueryValidator.MAX_LENGTH)
        assertIs<QueryValidation.Valid>(SearchQueryValidator.validate(atLimit))
    }

    @Test
    fun `strips control characters that break query strings`() {
        val normalized = SearchQueryValidator.normalize("beat\u0000les\u0007")
        assertEquals("beatles", normalized)
        assertTrue(normalized.none { it.code < 0x20 })
    }
}
