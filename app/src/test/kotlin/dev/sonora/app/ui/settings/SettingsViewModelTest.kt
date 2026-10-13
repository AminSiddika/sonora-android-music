package dev.sonora.app.ui.settings

import dev.sonora.app.data.ThemeMode
import dev.sonora.app.testing.FakeRecentlyPlayedRepository
import dev.sonora.app.testing.FakeUserPreferences
import dev.sonora.app.testing.FakeYouTubeKeyStore
import dev.sonora.core.domain.InMemoryFavoritesRepository
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.domain.YouTubeKeyResolution
import dev.sonora.core.domain.YouTubeKeySource
import dev.sonora.core.model.Track
import dev.sonora.core.model.YouTubeAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val track = Track(id = 3, title = "Late Night", artist = "Sonora Test")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        keyStore: FakeYouTubeKeyStore = FakeYouTubeKeyStore(),
        keyFlow: MutableStateFlow<YouTubeKeyResolution> = MutableStateFlow(YouTubeApiKey.resolve(null, null)),
        preferences: FakeUserPreferences = FakeUserPreferences(),
        favorites: InMemoryFavoritesRepository = InMemoryFavoritesRepository(),
        recent: FakeRecentlyPlayedRepository = FakeRecentlyPlayedRepository(),
    ) = SettingsViewModel(
        preferences = preferences,
        favoritesRepository = favorites,
        recentlyPlayedRepository = recent,
        youTubeKeyStore = keyStore,
        youTubeKey = keyFlow,
        versionName = "1.0.0",
    )

    @Test
    fun `a valid key is stored on the device`() = runTest(dispatcher) {
        val keyStore = FakeYouTubeKeyStore()
        val viewModel = viewModel(keyStore = keyStore)
        advanceUntilIdle()

        viewModel.onKeyInputChange("AIzaSyD-1234567890abcdefghijklmnopq")
        viewModel.saveKey()
        advanceUntilIdle()

        assertEquals("AIzaSyD-1234567890abcdefghijklmnopq", keyStore.key.first())
        assertEquals("", viewModel.state.value.keyInput)
        assertNull(viewModel.state.value.keyError)
        assertTrue(viewModel.state.value.keySaved)
    }

    @Test
    fun `a placeholder is rejected before it can be saved`() = runTest(dispatcher) {
        val keyStore = FakeYouTubeKeyStore()
        val viewModel = viewModel(keyStore = keyStore)
        advanceUntilIdle()

        viewModel.onKeyInputChange("YOUR_API_KEY")
        viewModel.saveKey()
        advanceUntilIdle()

        assertEquals(YouTubeKeyError.NOT_A_KEY, viewModel.state.value.keyError)
        assertNull(keyStore.key.first())
    }

    @Test
    fun `an empty key is rejected`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.saveKey()
        advanceUntilIdle()

        assertEquals(YouTubeKeyError.EMPTY, viewModel.state.value.keyError)
    }

    @Test
    fun `typing clears a previous error`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.saveKey()
        advanceUntilIdle()
        assertEquals(YouTubeKeyError.EMPTY, viewModel.state.value.keyError)

        viewModel.onKeyInputChange("AIza")
        assertEquals(null, viewModel.state.value.keyError)
    }

    @Test
    fun `removing the key clears the store`() = runTest(dispatcher) {
        val keyStore = FakeYouTubeKeyStore("AIzaSyStoredKey1234567890abcdefghij")
        val viewModel = viewModel(keyStore = keyStore)
        advanceUntilIdle()

        viewModel.removeKey()
        advanceUntilIdle()

        assertNull(keyStore.key.first())
    }

    @Test
    fun `the youtube status follows the key flow`() = runTest(dispatcher) {
        val keyFlow = MutableStateFlow(YouTubeApiKey.resolve(null, null))
        val viewModel = viewModel(keyFlow = keyFlow)
        advanceUntilIdle()

        assertEquals(YouTubeAvailability.MissingKey, viewModel.state.value.youTubeKey.availability)
        assertEquals(YouTubeKeySource.NONE, viewModel.state.value.youTubeKey.source)

        keyFlow.value = YouTubeKeyResolution(
            key = "AIzaSyRuntimeKey1234567890abcdefghij",
            availability = YouTubeAvailability.Available,
            source = YouTubeKeySource.RUNTIME,
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.youTubeKey.availability.isAvailable)
        assertEquals(YouTubeKeySource.RUNTIME, viewModel.state.value.youTubeKey.source)
    }

    @Test
    fun `local data counters and history clearing work`() = runTest(dispatcher) {
        val favorites = InMemoryFavoritesRepository()
        val recent = FakeRecentlyPlayedRepository()
        val viewModel = viewModel(favorites = favorites, recent = recent)
        favorites.add(track)
        recent.record(track)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.favoritesCount)
        assertEquals(1, viewModel.state.value.historyCount)

        viewModel.clearHistory()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.historyCount)
        assertTrue(viewModel.state.value.historyCleared)

        viewModel.acknowledgeHistoryCleared()
        assertTrue(!viewModel.state.value.historyCleared)
    }

    @Test
    fun `appearance preferences are written through`() = runTest(dispatcher) {
        val preferences = FakeUserPreferences()
        val viewModel = viewModel(preferences = preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setReduceMotion(true)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        assertTrue(preferences.reduceMotion.first())
    }
}
