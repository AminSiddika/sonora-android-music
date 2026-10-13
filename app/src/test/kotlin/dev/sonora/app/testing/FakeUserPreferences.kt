package dev.sonora.app.testing

import dev.sonora.app.data.ThemeMode
import dev.sonora.app.data.UserPreferences
import dev.sonora.core.model.SearchProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory preferences used by unit tests and previews. */
class FakeUserPreferences(
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
    initialReduceMotion: Boolean = false,
    initialProvider: SearchProvider = SearchProvider.default,
) : UserPreferences {

    private val theme = MutableStateFlow(initialTheme)
    private val motion = MutableStateFlow(initialReduceMotion)
    private val provider = MutableStateFlow(initialProvider)

    override val themeMode: Flow<ThemeMode> = theme

    override val reduceMotion: Flow<Boolean> = motion

    override val lastProvider: Flow<SearchProvider> = provider

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        motion.value = enabled
    }

    override suspend fun setLastProvider(provider: SearchProvider) {
        this.provider.value = provider
    }
}
