# Sonora — Android music discovery & preview player

[![Android CI](https://github.com/AminSiddika/sonora-android-music/actions/workflows/android-ci.yml/badge.svg)](https://github.com/AminSiddika/sonora-android-music/actions/workflows/android-ci.yml)

Sonora is a native Android music discovery app built with **Kotlin** and **Jetpack Compose** (Material 3).
It searches Apple's public **iTunes Search API** for songs and plays the **30 second previews** Apple
supplies for them. Optionally — and only when an API key is configured — it can also search
**YouTube** through the YouTube Data API v3 and hand playback over to the official YouTube
experience.

The catalogue experience works with **no API key, no account and no sign-in**.

---

## Contents

- [Features](#features)
- [Preview playback, attribution and legal notes](#preview-playback-attribution-and-legal-notes)
- [Screens](#screens)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [YouTube API key setup](#youtube-api-key-setup)
- [API behaviour and limitations](#api-behaviour-and-limitations)
- [Privacy](#privacy)
- [Continuous integration](#continuous-integration)
- [Testing](#testing)
- [Accessibility, responsiveness and motion](#accessibility-responsiveness-and-motion)
- [Project layout](#project-layout)
- [Verification status](#verification-status)
- [Third-party assets](#third-party-assets)

---

## Features

**Discovery**

- Home screen with a curated "shelf of the day" that rotates deterministically every day.
- Mood/genre chips and quick searches (Lo-fi, Jazz, Classical, Synthwave, Soul, …).
- Locally stored favourites and a short listening history, so the screen still has content offline.

**Search**

- One search field, two clearly separated providers: **Catalogue** (iTunes, always available) and
  **YouTube** (optional, needs a key).
- Keyboard submit (`ImeAction.Search`), quick-search chips, result counts, and per-item badges.
- Graceful handling of every failure mode: loading skeletons, empty results, offline, timeout,
  provider rate limiting, missing/invalid key, quota exhaustion, server errors and malformed
  payloads — each with a useful recovery action.

**Playback (previews)**

- Media3/ExoPlayer playback of Apple's `previewUrl` only, with audio-focus handling
  (`handleAudioFocus = true`) and automatic pause when headphones are unplugged.
- Persistent mini player across every screen, plus a full player with scrubber, previous/next,
  shuffle, repeat (off/all/one) and favourite toggle.
- Queue logic (shuffle order, repeat rules, removal, wrap-around) lives in pure Kotlin and is unit
  tested.
- Clear "Preview • 0:30" badges; tracks with no preview are shown as unplayable with an explanation.

**Favourites and settings (persisted locally)**

- Favourites, listening history, theme (system/light/dark) and reduced-motion preference are stored
  in a single DataStore Preferences file — no third-party database, no cloud sync.
- Duplicate-safe favourite toggling, history capped at 12 entries, "clear history" action.

**YouTube integration (optional, two independent routes)**

- **Search** — YouTube Data API v3 `search.list` (title, channel, thumbnail, description). Needs an
  API key; the app runs catalogue-only without one.
- **Playback in the app** — Google's **official IFrame Player API** embedded in a WebView
  (`androidyoutubeplayer`), exactly what a browser would run. No API key, no stream extraction:
  YouTube serves its own video, ads and controls, and playback stops when you leave the screen.
- **Playback outside the app** — "Watch on YouTube" opens the official YouTube app (`vnd.youtube:`)
  and falls back to the mobile website.
- Required attribution and policy links (YouTube Terms of Service, Google Privacy Policy) surface in
  the UI.

---

## Preview playback, attribution and legal notes

- Sonora is **not** a full-track streaming service. It plays only the legally provided 30 second
  previews that the iTunes Search API returns in `previewUrl`, and it labels them as previews in the
  UI. Tracks without a preview are shown as unplayable instead of being faked.
- Catalogue data, artwork and previews come from Apple's public iTunes Search API. Artwork URLs are
  upscaled by rewriting the size token Apple embeds in the file name (`100x100bb.jpg` → `600x600bb.jpg`).
- YouTube results come from the YouTube Data API v3. Sonora does not resolve stream URLs, does not
  download media, does not bypass ads or restrictions, and does not play YouTube audio in the
  background. "Watch on YouTube" opens the official app when installed, otherwise the mobile website.
- YouTube branding/attribution plus links to the YouTube Terms of Service and the Google Privacy
  Policy are shown on the search results, the video details screen and the settings screen.

---

## Screens

| Screen | What it does |
| --- | --- |
| **Home** | Shelf of the day, mood/genre chips, favourites strip, "jump back in" history, disclosures. |
| **Search** | Query field, catalogue/YouTube switch, results, and all empty/error/offline states. |
| **Track details** | Large artwork, metadata, preview play, favourite toggle, "Open in Apple Music", preview disclosure. |
| **Video details** | Thumbnail, channel, description, "Watch on YouTube", channel link, policy links. |
| **Favourites** | Locally stored favourites, "play all", remove, empty state. |
| **Player** | Full-screen player overlay: artwork, scrubber, transport, shuffle/repeat, failure messaging. |
| **Mini player** | Persistent bar above the navigation with progress, play/pause, next, expand and stop. |
| **Settings** | Theme, reduce motion, local data counts, clear history, YouTube status/policy, privacy, about. |

---

## Architecture

```
:core   Pure Kotlin/JVM module - domain + data (no Android dependency)
:app    Android app - Compose UI, Media3 playback, DataStore persistence
```

**`:core` (unit tested on the JVM, no Android SDK required)**

- `model` — `Track`, `VideoItem`, `SonoraError`, `ApiResult`, `SearchProvider`, `YouTubeAvailability`.
- `domain` — `SearchRepository` (query validation, provider routing, 5 minute TTL cache),
  `PlaybackQueue` (immutable shuffle/repeat rules), `ArtworkUrls`, `DurationFormat`,
  `ExpiringCache`, `FavoritesRepository` (+ in-memory implementation), `DiscoveryShelves`,
  `YouTubeApiKey`, `ExternalLinks`.
- `net` — Ktor clients for the iTunes Search API and the YouTube Data API, with an
  `HttpErrorMapper` that converts HTTP status codes, timeouts, IO failures and serialization errors
  into the typed `SonoraError` values used by the UI.

**`:app`**

- `di/SonoraGraph` — a hand written graph (a dozen nodes, so no DI framework): HTTP client,
  repositories, DataStore stores, in-memory catalogue session, network monitor, player, launcher.
- `player/SonoraPlayer` — ExoPlayer + MediaSession wrapper that owns audio focus, preview loading,
  queue navigation, completion handling and the `PlayerUiState` stream.
- `ui/*` — Compose screens, components and view models (`SearchViewModel`, `HomeViewModel`,
  `FavoritesViewModel`, `PlayerViewModel`, `SettingsViewModel`, details view models).
- `data/*` — DataStore-backed favourites/history/preferences, connectivity monitor, YouTube intent
  launcher.

Coroutines and `Flow` are used throughout; view models expose immutable `StateFlow` UI state;
`ui/ErrorPresentations.kt` maps each `SonoraError` to localized copy and a recovery action.

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Requirements

| Tool | Version used here | Notes |
| --- | --- | --- |
| JDK | **17** | Required by AGP 9; the Gradle wrapper and CI use Temurin 17. JDK 21 also runs the build. |
| Gradle | **9.8.1** (wrapper included) | `./gradlew` uses the checked-in wrapper. |
| Android Gradle Plugin | **9.4.1** | Built-in Kotlin support; no `kotlin-android` plugin is applied. |
| Kotlin | **2.4.21** | Compose compiler and serialization plugins are pinned to the same version. |
| Compose | BOM **2026.09.00** (Compose 1.12.1, Material 3 1.4.0) | |
| compileSdk / targetSdk | **37** | SDK package `platforms;android-37.0`, build tools `36.0.0`. |
| minSdk | **26** | Android 8.0 and newer. |

---

## Build and run

```bash
# 1. Point the build at your Android SDK (this file is git-ignored)
cat > local.properties <<'EOF'
sdk.dir=/path/to/Android/sdk
EOF

# 2. Unit tests for the domain/data layer (runs on the JVM, no emulator)
./gradlew :core:test

# 3. Everything the CI job runs
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# 4. Install the debug build on a connected device
./gradlew :app:installDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Open the project in Android Studio (Ladybug/Meerkat or newer) and run the `app` configuration to use
the Compose previews and the debugger.

There is no release signing configuration in the repository — deliberately: no keystore or
credential is ever committed.

---

## YouTube API key setup

YouTube search is **optional**. Without a key, Sonora runs in catalogue-only mode: the YouTube tab
explains how to enable it and offers a one-tap switch back to the catalogue.

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/) and enable
   **YouTube Data API v3**.
2. Create an API key.
3. Restrict the key. Because Sonora is a client-side app, use **Android app restrictions**
   (package name `dev.sonora.app` plus your signing certificate SHA-1) and restrict the key to the
   YouTube Data API v3. A client-side key is always extractable from the APK, which is exactly why
   the restriction matters — never use an unrestricted key.
4. Provide the key in one of these ways (resolution order):

   | Source | How |
   | --- | --- |
   | Gradle property | `./gradlew :app:assembleDebug -Pyoutube.api.key=YOUR_KEY` or a line in `gradle.properties` |
   | Environment variable | `export YOUTUBE_API_KEY=YOUR_KEY` |
   | Local file | `youtube.api.key=YOUR_KEY` in the untracked `local.properties` |

   The value is injected through the AGP variant API as `BuildConfig.YOUTUBE_API_KEY`. It is never
   read from a tracked file, never logged, and `local.properties` is git-ignored.

5. **Or paste it in the app (no rebuild):** Settings → *YouTube search* → paste the key → **Save key**.
   The key is stored in this app's private DataStore file, is excluded from Android backup, and takes
   precedence over a build-time key. **Remove key** deletes it again.

6. **GitHub Actions:** add a repository secret named `YOUTUBE_API_KEY`
   (Settings → Secrets and variables → Actions → New repository secret). The workflow passes it to
   the build only for the assemble step. Without the secret, CI still builds and uploads the APK in
   catalogue-only mode.

Placeholders such as `YOUR_API_KEY` are rejected by `YouTubeApiKey.sanitize`, so a copy-pasted
template does not silently produce broken requests.

### API key vs OAuth client secret

A file named `client_secret_<project>-<id>.apps.googleusercontent.com.json` is a **Google OAuth 2.0
installed-app client**, not an API key. It contains `client_id`, `client_secret` and `redirect_uris`
and cannot call an API on its own: it only identifies the app during a browser consent flow that the
account owner has to complete, after which the app receives an access token. For public, read-only
`search.list` calls an **API key is the right credential** and needs no sign-in. If you own such a
JSON file, create an API key in the *same* Google Cloud project instead
(APIs & Services → Credentials → Create credentials → API key), and keep the OAuth client out of the
repository.

### Why Sonora does not use a stream extractor

Popular open-source YouTube clients (for example RiPlay) combine two very different approaches:
`androidyoutubeplayer` (the official IFrame player — the part Sonora uses for in-app playback) and a
stream extractor such as NewPipeExtractor. Sonora intentionally does **not** use an extractor:
resolving `googlevideo` stream URLs, proxying media, stripping ads or playing YouTube audio in the
background violates the YouTube API Services Terms and the YouTube Terms of Service, breaks whenever
Google rotates player ciphers, and would make this project's compliance claims untrue. If you need
in-app playback, the embedded official player above gives it without touching any of that.

---

## API behaviour and limitations

**iTunes Search API (`https://itunes.apple.com/search`)**

- No key required, `media=music` + `entity=song`, artwork and 30 second preview URLs.
- Documented limit is roughly **20 requests per minute per IP**. Sonora mitigates this with a
  5 minute in-memory cache, one request for the home shelf, and a dedicated rate-limit error state
  ("Slow down a moment") instead of a crash.
- The API has **no browsing/trending endpoint**. Discovery is therefore expressed as curated search
  terms; the home screen issues at most one search request.
- Some storefronts simply have no preview for a track — those items stay listed and are marked
  "No preview" and cannot be played.
- Tested failure matrix: 429/403 → rate limited, 408/socket timeout → timeout, 5xx → server error
  with status code, invalid JSON → "unexpected response", DNS/IO failure → offline state.

**YouTube Data API v3 (`search.list`)**

- Only `part=snippet`, `type=video`, `safeSearch=moderate`, `videoEmbeddable=true` and a `fields`
  filter are requested. Sonora never requests stream URLs or `contentDetails` for playback.
- Errors are mapped by reason: `quotaExceeded` → quota state, `keyInvalid`/`badRequest` →
  invalid-key state with a link to the setup guide, `rateLimitExceeded` → rate-limited state.
- Playback always continues in the official YouTube app or website; nothing is downloaded,
  re-streamed, cached, backgrounded or ad-modified.

---

## Privacy

- No account, no analytics SDK, no advertising SDK, no crash reporting.
- Search terms go directly from the device to Apple (iTunes Search API) and, only when YouTube search
  is enabled, to Google (YouTube Data API v3). Nothing passes through a server owned by this project.
- Favourites, listening history and preferences are stored locally in a private
  DataStore file (`files/datastore/sonora_preferences.preferences_pb`). Backup rules include only
  that folder.
- No API key, token or signing credential is committed to the repository.

---

## Continuous integration

`.github/workflows/android-ci.yml` runs on every push to `main`, on every pull request and on manual
dispatch. It:

1. checks out the repository and sets up **JDK 17**,
2. installs the Android SDK platform `android-37.0` and build tools `36.0.0`,
3. runs `./gradlew :core:test` (domain, data, API and error-handling tests),
4. runs `./gradlew :app:testDebugUnitTest` (view models and player state),
5. runs `./gradlew :app:lintDebug`,
6. builds the debug APK with `./gradlew :app:assembleDebug` (passing `YOUTUBE_API_KEY` when the
   secret exists),
7. uploads `sonora-debug-apk` and the test/lint reports as workflow artifacts.

No private credential is needed for a green build.

---

## Testing

Local unit tests (JVM, no emulator or SDK required):

```bash
./gradlew :core:test
```

| Suite | Covers |
| --- | --- |
| `ITunesCatalogTest` | Parsing (fixtures), artwork upscaling, preview-less tracks, explicit flag, query parameters, limit clamping, 429/403/500, malformed JSON, offline, timeouts |
| `YouTubeCatalogTest` | Parsing, thumbnail fallback, compliant parameter set, quota vs invalid key vs server errors, malformed JSON |
| `SearchRepositoryTest` | Query validation, provider routing, cache hits/TTL, `forceRefresh`, missing/placeholder key behaviour |
| `PlaybackQueueTest` | Next/previous wrap-around, repeat off/all/one, deterministic shuffle, removal, queue replacement |
| `ExpiringCacheTest` | TTL expiry, LRU eviction, clearing |
| `HttpErrorMapperTest` | Status/exception → typed error mapping |
| `SearchQueryValidatorTest`, `ArtworkUrlsTest`, `DurationFormatTest`, `YouTubeApiKeyTest`, `ExternalLinksTest`, `FavoritesRepositoryTest` | Validation, URL rewriting, formatting, key sanitising, policy links, favourite toggling and serialization round-trip |

App module (runs in CI with the Android SDK available):

| Suite | Covers |
| --- | --- |
| `SearchViewModelTest` | Success, empty, error, blank query, YouTube missing-key path, provider switching and persistence, offline state, favourites |
| `HomeViewModelTest` | Shelf rotation and load, shelf failure, favourites/history shelves, offline flag |
| `PlayerUiStateTest` | Progress clamping, remaining time, active/queue flags, safe defaults |

---

## Accessibility, responsiveness and motion

- Every icon and image carries a meaningful `contentDescription`; decorative icons are marked `null`.
- Headings use `semantics { heading() }`, the mini player uses a polite live region, and custom
  labels (favourite add/remove, play/next/previous, expand/close player) are localized strings.
- Touch targets follow Material 3 minimums (48dp), rows are keyboard/switch-accessible, and the
  search field submits from the IME.
- Layouts adapt: phones use a bottom navigation bar, wider screens and landscape use a navigation
  rail; artwork scales with `aspectRatio` and content sits inside a scroll container.
- Light and dark themes are hand-tuned for contrast; the palette is fixed so the app looks the same
  everywhere it is installed.
- Reduced motion: the in-app "Reduce motion" switch, the system animator duration scale
  (`ANIMATOR_DURATION_SCALE = 0`) and `ValueAnimator.areAnimatorsEnabled()` all collapse transitions
  and the loading shimmer to zero duration.

---

## Project layout

```
sonora-android-music/
├── .github/workflows/android-ci.yml     # tests, lint, debug APK artifact
├── app/                                 # Android app module (Compose, Media3, DataStore)
│   ├── build.gradle.kts                 # AGP 9 DSL, variant API BuildConfig injection
│   ├── proguard-rules.pro
│   └── src/main/kotlin/dev/sonora/app/  # di, data, player, ui (theme, components, screens)
├── core/                                # Pure Kotlin: domain + data + API clients
│   └── src/{main,test}/kotlin/dev/sonora/core/
├── docs/ARCHITECTURE.md
├── gradle/libs.versions.toml            # single source of truth for versions
├── gradle/wrapper/                      # Gradle 9.8.1 wrapper
├── local.properties.example             # SDK path + optional YouTube key template
├── licenses/Poppins-OFL.txt             # font licence (OFL 1.1)
└── README.md
```

---

## Verification status

All of the following were actually executed for this project:

| Command | Result |
| --- | --- |
| `./gradlew :core:test` | **PASS** — 84 tests, 0 failures, 0 errors across 12 suites |
| `./gradlew :app:testDebugUnitTest` | **PASS** — 21 tests, 0 failures (`SearchViewModelTest`, `HomeViewModelTest`, `PlayerUiStateTest`) |
| `./gradlew :app:lintDebug` | **PASS** with `abortOnError = true`; one advisory warning remains (`ObsoleteSdkInt` for the conventional `mipmap-anydpi-v26` adaptive-icon folder, kept deliberately) |
| `./gradlew clean … :app:assembleDebug` | **PASS** — clean full build in 9m32s on an aarch64 host |
| APK inspection | `app-debug.apk` (25.0 MiB) reports `package=dev.sonora.app.debug`, `minSdkVersion=26`, `targetSdkVersion=37`, `application-label='Sonora'`, multi-dex, no API key embedded |
| GitHub Actions `Android CI` | **PASS** — run [#38367950422](https://github.com/AminSiddika/sonora-android-music/actions/runs/38367950422) uploads the `sonora-debug-apk` and `sonora-reports` artifacts |

Verification was possible on an aarch64 Android/Termux host by extracting
`platforms;android-37.0` and `build-tools;36.0.0` and pointing the build at the platform `aapt2`:

```bash
./gradlew :app:assembleDebug \
  -Pandroid.aapt2FromMavenOverride=$PREFIX/bin/aapt2
```

On a normal desktop, `gradlew` uses the aapt2 from the Android SDK and no override is needed.

---

## Third-party assets

- **Poppins** font (SIL Open Font License 1.1) — `app/src/main/res/font/`, licence in
  `licenses/Poppins-OFL.txt`.
- Catalogue data, artwork and previews © Apple, via the public iTunes Search API.
- YouTube search results © Google/YouTube, subject to the YouTube API Services Terms of Service.

Code in this repository is released under the MIT License (see `LICENSE`).
