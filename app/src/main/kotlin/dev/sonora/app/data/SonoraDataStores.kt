package dev.sonora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Single DataStore file for the small amount of local user data Sonora keeps:
 * favourites, the recent listening history and UI preferences. Preferences
 * DataStore ships with AndroidX, so no third party database is needed.
 */
internal val Context.sonoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonora_preferences")

private val favoritesKey = stringPreferencesKey("favorites_json")
private val recentlyPlayedKey = stringPreferencesKey("recently_played_json")

private val lenientJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class DataStoreFavoritesRepository(context: Context) : FavoritesRepository {

    private val dataStore = context.applicationContext.sonoraDataStore

    override val favorites: Flow<List<Track>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> decodeTracks(preferences[favoritesKey]) }

    override suspend fun toggle(track: Track): Boolean {
        var isFavorite = false
        dataStore.edit { preferences ->
            val current = decodeTracks(preferences[favoritesKey])
            val alreadyFavorite = current.any { it.id == track.id }
            val updated = if (alreadyFavorite) {
                current.filterNot { it.id == track.id }
            } else {
                listOf(track) + current.filterNot { it.id == track.id }
            }
            isFavorite = !alreadyFavorite
            preferences[favoritesKey] = lenientJson.encodeToString(updated)
        }
        return isFavorite
    }

    override suspend fun add(track: Track) {
        dataStore.edit { preferences ->
            val current = decodeTracks(preferences[favoritesKey])
            preferences[favoritesKey] = lenientJson.encodeToString(
                listOf(track) + current.filterNot { it.id == track.id },
            )
        }
    }

    override suspend fun remove(trackId: Long) {
        dataStore.edit { preferences ->
            val current = decodeTracks(preferences[favoritesKey])
            preferences[favoritesKey] = lenientJson.encodeToString(current.filterNot { it.id == trackId })
        }
    }
}

/** Keeps a short "jump back in" list for the discovery screen. */
class DataStoreRecentlyPlayedRepository(context: Context) : RecentlyPlayedRepository {

    private val dataStore = context.applicationContext.sonoraDataStore

    override val recentlyPlayed: Flow<List<Track>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> decodeTracks(preferences[recentlyPlayedKey]) }

    override suspend fun record(track: Track) {
        dataStore.edit { preferences ->
            val current = decodeTracks(preferences[recentlyPlayedKey])
            val updated = (listOf(track) + current.filterNot { it.id == track.id }).take(MAX_ENTRIES)
            preferences[recentlyPlayedKey] = lenientJson.encodeToString(updated)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(recentlyPlayedKey) }
    }

    private companion object {
        const val MAX_ENTRIES = 12
    }
}

private fun decodeTracks(raw: String?): List<Track> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { lenientJson.decodeFromString<List<Track>>(raw) }.getOrDefault(emptyList())
}
