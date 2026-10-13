# Sonora architecture notes

This document explains the decisions behind the Sonora codebase: how the modules fit together, why
the YouTube integration is deliberately narrow, and which parts are unit tested on the JVM versus in
CI.

## Module graph

```
┌──────────────────────────────┐        ┌──────────────────────────────────────┐
│ :app  (Android, AGP 9.4.1)   │        │ :core (Kotlin/JVM, no Android)       │
│                              │        │                                      │
│ di/SonoraGraph ──────────────┼───────▶ SearchRepository ─┐                   │
│ data/* (DataStore, network)  │        │                    ├─ ITunesCatalog   │
│ player/SonoraPlayer (Media3) │        │                    └─ YouTubeCatalog  │
│ ui/* (Compose + ViewModels)  │        │ PlaybackQueue, ArtworkUrls, models   │
└──────────────────────────────┘        └──────────────────────────────────────┘
```

`:core` has **no Android dependency**, so all of the interesting business logic (query validation,
provider routing, caching, error mapping, JSON parsing, queue/shuffle rules, formatting) can be unit
tested with `./gradlew :core:test` on any machine with a JDK — no emulator, no Android SDK. `:app`
depends on `:core` via `implementation(project(":core"))`.

## Why a JVM module rather than an Android library

- The JVM module can be verified in environments where the Android SDK is unavailable.
- Ktor's `MockEngine` makes HTTP behaviour (status codes, malformed payloads, timeouts, offline)
  testable without a device.
- Nothing in `:core` needs Android APIs: Ktor's OkHttp engine works on both JVM and Android.

## Layers

| Layer | Where | Responsibility |
| --- | --- | --- |
| Domain models | `core/model` | `Track`, `VideoItem`, `SonoraError`, `ApiResult`, `SearchProvider` |
| Domain services | `core/domain` | `SearchRepository`, `PlaybackQueue`, `ExpiringCache`, `ArtworkUrls`, `DurationFormat`, `DiscoveryShelves`, `YouTubeApiKey`, `ExternalLinks`, favourites interfaces |
| Data / network | `core/net` | iTunes + YouTube clients, `SonoraHttp` client factory, `HttpErrorMapper`, `apiCall` |
| Persistence | `app/data` | DataStore for favourites, history and preferences; in-memory catalogue session |
| Playback | `app/player` | `SonoraPlayer` (ExoPlayer + MediaSession), `PlayerUiState` |
| Presentation | `app/ui` | Compose screens/components, view models, theme, error copy mapping |

### Error handling contract

Every data call returns `ApiResult<T>`: `Success(data)` or `Failure(SonoraError)`. `SonoraError` is a
closed set (`NoConnection`, `Timeout`, `RateLimited`, `MissingApiKey`, `InvalidApiKey`,
`QuotaExceeded`, `MalformedResponse`, `ServerError`, `EmptyQuery`, `Unknown`), which means:

- the UI never inspects HTTP codes or exception types,
- missing copy for a new error case is a compile error rather than a blank screen,
- tests assert on typed values instead of message strings.

`ui/ErrorPresentations.kt` is the single place where errors become localized text plus a recovery
action (retry / open the YouTube setup guide / switch to the catalogue).

### Caching and rate limiting

The iTunes Search API allows roughly 20 requests per minute per IP. `SearchRepository` keeps an
LRU-bounded, 5 minute TTL cache keyed by provider + limit + normalised query. Errors are never
cached, so a retry can succeed. The home screen performs at most one catalogue request, and the
"Slow down a moment" error state explains the limit instead of pretending the app is broken.

### Playback model

`SonoraPlayer` wraps a single `ExoPlayer` instance created by the application-scoped graph, so the
mini player survives navigation. It sets `AudioAttributes` for media/music with
`handleAudioFocus = true`, and `setHandleAudioBecomingNoisy(true)`.

`PlaybackQueue` (in `:core`) is immutable: `next()`, `previous()`, `toggleShuffle()`,
`cycleRepeat()` and `remove(trackId)` return new instances, which makes the shuffle/repeat semantics
deterministic and trivially testable (including a seeded `Random` so tests can assert exact orders).

Completion handling: repeat-one replays, repeat-all wraps to the first item, otherwise the queue
advances and stops at the end. Preview-only playback is why Sonora does **not** run a foreground
media service or post playback notifications — that is documented as a deliberate limitation rather
than an oversight.

### Dependency injection

`SonoraGraph` is a plain class constructed once in `SonoraApplication`. With roughly a dozen nodes a
DI framework would add build complexity without removing any real wiring code. View models receive
their collaborators explicitly (interfaces from `:core` where possible), which keeps them testable
with fakes such as `InMemoryFavoritesRepository` and `FakeUserPreferences`.

### Navigation

Navigation Compose type-safe routes (`@Serializable` route classes) move only ids. Detail screens
resolve their payload from the in-memory `CatalogSession`, falling back to favourites and history, so
a process restore does not need a second network call and an id can never be stale-but-crashing: a
missing item renders an explicit "Track unavailable" state.

### YouTube policy boundary

The YouTube provider is intentionally limited to `search.list`. There is no ExoPlayer path for
YouTube media, no `get_video_info`-style extraction, no player-API override, no background playback.
`YouTubeLauncher` resolves `vnd.youtube:<id>` first and falls back to the HTTPS watch URL. The
required attribution and policy links are rendered by the UI itself, and the same links are listed in
the README.

## Testing strategy

| Level | Location | Tooling |
| --- | --- | --- |
| Pure domain logic | `core/src/test` | kotlin.test + JUnit 4 |
| HTTP clients and error mapping | `core/src/test` | Ktor `MockEngine`, JSON fixtures in `core/src/test/resources/fixtures` |
| View models | `app/src/test` | kotlinx-coroutines-test with a `StandardTestDispatcher` replacing `Dispatchers.Main` |
| Build, lint, APK | GitHub Actions | `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` |

Fixtures are real-shaped API payloads (including a malformed entry that must be skipped, a track
without a preview, an explicit track and a YouTube item missing its `id`), so parsing rules are
verified against the awkward cases providers actually return.
