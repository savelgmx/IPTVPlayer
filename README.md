# IPTV Player — Android TV MVP

A minimal, remote-friendly IPTV player for Android TV: parses standard M3U
playlists, opens directly into a browsable channel grid with zero setup,
and plays streams through a buffer configuration tuned for live TV rather
than on-demand video.

Originally prototyped with the Leanback framework (Fragments +
`Presenter`-based View adapters); rewritten to a single-Activity Jetpack
Compose for TV app with Navigation Compose and plain MVVM. The domain and
data layers were untouched by that rewrite — see Architecture below for why
that's the point of the layering, not a coincidence.

## ⚠️ About the bundled playlist

The bundled `assets/default_playlist.m3u` contains **only public technical
test streams** (Apple's official HLS demo stream, Mux's public test
streams) — the same URLs used throughout the Android/iOS developer
community to test HLS players. It contains no copyrighted broadcast
channels. Sourcing a real playlist legally (a licensed IPTV subscription, a
personal media server, a broadcaster's own public stream) is the deploying
party's responsibility, same as with any video player.

## Architecture

Clean Architecture layering, unchanged by the Compose rewrite: `domain` has
zero Android dependencies and owns the repository interface + use cases;
`data` implements the interface; `presentation` (ViewModels, Composable
screens) depends only on use cases, never on the repository or data-layer
types directly.

```
presentation (ViewModels, Composable screens, NavHost)
        │  depends on
        ▼
domain/usecase (GetChannelsUseCase, SaveCustomPlaylistUrlUseCase, ...)
        │  depends on
        ▼
domain/repository (PlaylistRepository interface — pure Kotlin)
        ▲  implements
        │
data/repository (PlaylistRepositoryImpl — Context, SharedPreferences, asset/URL/file loading)
```

**Why the rewrite only touched `presentation`:** every ViewModel
(`ChannelsViewModel`, `PlaybackViewModel`, `SettingsViewModel`) depends on
use cases, not on Fragments, Activities, or any Leanback type — swapping
the UI framework underneath them required zero changes to `domain` or
`data`. That's the actual payoff of layering the app this way, not just a
diagram.

### MVVM, not MVP

The earlier version had a `ChannelCardPresenter` class — a naming
coincidence with the (unrelated, and largely superseded) Model-View-Presenter
UI pattern, not an intentional MVP architecture. It was a Leanback
`Presenter` (the base class Leanback's `RecyclerView`-like adapters require
for binding a card's View), which no longer exists in this version at all:
Compose screens observe `StateFlow` from a `@HiltViewModel` directly — plain
MVVM, no Presenter layer in between.

## Screens (single Activity, Navigation Compose)

| Route | Composable | Replaces |
|---|---|---|
| `channels` | `ChannelListScreen` | `ChannelGridFragment` + `ChannelCardPresenter` (Leanback) |
| `playback/{channelId}` | `PlaybackScreen` | `PlaybackActivity` |
| `settings` | `SettingsScreen` | `SettingsActivity` + `PlaylistSettingsFragment` (`GuidedStepSupportFragment`) |

`MainActivity` is the only Activity in the app; `AppNavHost` wires the three
destinations together via `NavController`.

## List, not grid

Channels render as a flat vertical list (`TvLazyColumn` + `tv-material`
`ListItem`: a leading circular logo or a fallback icon, channel name, and
group as a supporting line) rather than a tile grid — closer to how most
real IPTV apps and TV launchers present a long channel list, and more
scannable than square tiles once a playlist has hundreds of entries.

## Personal-use features

- **Custom M3U URL**, entered via a plain Compose text field and persisted
  across restarts. Falls back to the bundled test playlist if the saved URL
  fails to load, so a flaky personal server never leaves you on a blank screen.
- **Pick a local `.m3u` file** via the system document picker, or **load a
  file pushed via `adb`** into the app's own external files directory — see
  "Loading a real playlist" below.
- **In-player channel switching** — D-pad Up/Down (or a remote's dedicated
  Channel+/Channel- keys) moves to the next/previous channel without
  leaving the playback screen.
- **HLS/MPEG-TS auto-detection with fallback** — many real IPTV playlists
  point at extension-less URLs that are actually HLS manifests. Raw
  MPEG-TS is auto-detected by ExoPlayer's byte-sniffing regardless of
  extension; HLS without a `.m3u8` extension is retried once with an
  explicit MIME hint on the first playback error, rather than failing outright.
- **Tuned for real hardware, not just test streams**: `DefaultTrackSelector`
  requests tunneling (offloads A/V sync to hardware where supported — a
  common source of visible judder on live TS streams), and
  `DefaultRenderersFactory.setEnableDecoderFallback(true)` avoids hard
  failures on channels encoded with a profile the primary hardware decoder
  doesn't advertise support for.

## Loading a real playlist — three ways

The Settings screen (reachable via the "Playlist settings" row pinned first
in the list) offers three independent ways to point the app at a real M3U
playlist, because no single method is guaranteed to work on every Android
TV box:

| Method | Works when | Notes |
|---|---|---|
| **Paste a URL** | Always, if the server is reachable | Persisted, with automatic fallback to the bundled test streams if the URL stops responding. |
| **Pick a local file** | The device has a document picker/file manager installed | Uses the standard Storage Access Framework (`ACTION_OPEN_DOCUMENT`). Works reliably on the Android Studio emulator; **not guaranteed on stock TV firmware** — many Android TV/Google TV boxes ship with no document provider at all, so the picker may show nothing. |
| **Load from app folder** | Always — no picker, no permission needed | Reads a fixed filename (`IPTV.m3u`) from the app's own external files directory, which every app can read/write without any permission on any Android version (exempt from scoped storage restrictions). This is the one guaranteed-to-work path. |

To use the "Load from app folder" option:

```bash
adb push /path/to/your/IPTV.m3u /sdcard/Android/data/com.portfolio.iptvplayer/files/IPTV.m3u
```

Then open the app, go to Playlist settings, and select "Load IPTV.m3u from
app folder." Whichever method is used, the playlist's content is
immediately copied into the app's private internal storage — subsequent
app launches don't depend on the original URL still being up, the file
picker's grant still being valid, or the app-folder file still existing.

## Tech stack & why

| Choice | Why |
|---|---|
| **Jetpack Compose for TV** (`androidx.tv:tv-material`, `tv-foundation`) | The officially supported Google library for TV UI in Compose. `TvLazyColumn` gives D-pad focus restoration and scroll-to-focused-item for free — the same benefit Leanback's grid fragment used to provide — and `ListItem` handles the focus scale/highlight look without hand-rolled animation code. |
| **Navigation Compose** | Single Activity, three composable destinations, instead of three Activities and a Fragment transaction. |
| **Media3 ExoPlayer** for playback | Unchanged from the Leanback version — hardware-accelerated decode and adaptive bitrate (HLS) across wildly different TV hardware tiers. |
| **Coil** for channel logos | Compose-native image loading (coroutine-based `AsyncImage` composable). Glide's advantage under Leanback was bitmap pooling for `RecyclerView`/`GridView` view recycling; Compose's recomposition model and Coil's first-class Compose integration make Coil the more idiomatic fit now that there's no View-recycling layer to optimize around directly. |
| **Tuned `DefaultLoadControl`** | See below — this is the actual mechanism behind "no buffering." |

## How the buffering tuning works

`PlaybackScreen` configures ExoPlayer's `LoadControl` with:
- `minBufferMs = 20_000`, `maxBufferMs = 60_000` — the player keeps a large
  cushion of already-downloaded media, so short network hiccups don't empty
  the buffer and cause a visible stall.
- `bufferForPlaybackAfterRebufferMs = 5_000` — if a stall *does* happen, the
  player waits for a solid 5-second cushion before resuming, rather than
  resuming immediately and stalling again a second later.

The trade-off is a slightly longer initial buffering screen (a couple of
seconds) in exchange for far fewer stalls once playback is running — the
right trade for a "channel" experience where nobody is scrubbing a timeline.

## Deliberately out of scope

- **EPG / program guide** — this is a channel-switcher, not a guide
- **Favorites, search, channel groups as separate sections** — the flat
  list is intentional for "most simplified interface"; grouping by
  `group-title` (already parsed) is a natural next step for a
  multi-hundred-channel playlist
- **Persisted playback position / catch-up TV** — this targets live streams

## Setup

Open in Android Studio, sync Gradle, run on an Android TV emulator or
device (or a Fire TV / Google TV box in developer mode). No API keys or
backend needed — the bundled playlist plays immediately.
