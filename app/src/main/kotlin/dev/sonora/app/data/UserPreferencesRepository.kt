package dev.sonora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.sonora.core.model.SearchProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStorage(raw: String?): ThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

/**
 * Small user preference surface: theme, motion and the last used provider.
 * Defined as an interface so view models stay testable without Android.
 */
interface UserPreferences {

    val themeMode: Flow<ThemeMode>

    val reduceMotion: Flow<Boolean>

    val lastProvider: Flow<SearchProvider>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setLastProvider(provider: SearchProvider)
}

class DataStoreUserPreferences(context: Context) : UserPreferences {

    private val dataStore: DataStore<Preferences> = context.applicationContext.sonoraDataStore

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> ThemeMode.fromStorage(preferences[THEME_MODE]) }

    override val reduceMotion: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[REDUCE_MOTION] ?: false }

    override val lastProvider: Flow<SearchProvider> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            SearchProvider.entries.firstOrNull { it.name == preferences[LAST_PROVIDER] } ?: SearchProvider.default
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[REDUCE_MOTION] = enabled }
    }

    override suspend fun setLastProvider(provider: SearchProvider) {
        dataStore.edit { preferences -> preferences[LAST_PROVIDER] = provider.name }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val LAST_PROVIDER = stringPreferencesKey("last_search_provider")
    }
}
