package dev.sonora.core

/** Loads JSON fixtures from `src/test/resources/fixtures`. */
internal object TestFixtures {

    fun text(name: String): String {
        val stream = TestFixtures::class.java.getResourceAsStream("/fixtures/$name")
            ?: error("Fixture not found on the test classpath: $name")
        return stream.use { it.readBytes().decodeToString() }
    }
}
