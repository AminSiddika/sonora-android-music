package dev.sonora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Storage for the optional YouTube Data API v3 key a user pastes into Settings.
 *
 * Deliberately kept in its own DataStore file (`sonora_secrets`) which is
 * excluded from Android backup and device transfer: a credential must not leave
 * the device, while favourites and preferences still can.
 */
interface YouTubeKeyStore {

    /** The raw key the user saved, or null when none is stored. */
    val key: StateFlow<String?>

    suspend fun save(raw: String)

    suspend fun clear()
}

internal val Context.sonoraSecretsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonora_secrets")

class DataStoreYouTubeKeyStore(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : YouTubeKeyStore {

    private val dataStore: DataStore<Preferences> = context.applicationContext.sonoraSecretsDataStore

    private val _key = MutableStateFlow<String?>(null)
    override val key: StateFlow<String?> = _key.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { preferences -> _key.value = preferences[KEY] }
        }
    }

    override suspend fun save(raw: String) {
        val trimmed = raw.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(KEY)
            } else {
                preferences[KEY] = trimmed
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("youtube_api_key")
    }
}
