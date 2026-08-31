# IPTV Player — Android TV MVP

A minimal, remote-friendly IPTV player for Android TV: parses standard M3U
playlists, opens directly into a browsable channel grid with zero setup,
and plays streams through a buffer configuration tuned for live TV rather
than on-demand video.

## ⚠️ About the bundled playlist

The bundled `assets/default_playlist.m3u` contains **only public technical
test streams** (Apple's official HLS demo stream, Mux's public test
streams) — the same URLs used throughout the Android/iOS developer
community to test HLS players. It contains no copyrighted broadcast
channels. `PlaylistRepository.loadPlaylistFromUrl()` is the extension point
for pointing the app at a real playlist; sourcing that playlist legally
(a licensed IPTV subscription, a personal media server, a broadcaster's own
public stream) is the deploying party's responsibility, same as with any
video player.

## Architecture

Follows the same Clean Architecture layering as the other portfolio
projects — `domain` has zero Android dependencies and owns the repository
interface + use cases; `data` implements the interface; `presentation`
(ViewModels, Fragments/Activities) depends only on use cases, never on the
repository or data-layer types directly.

```
presentation (ViewModels, Fragments)
        │  depends on
        ▼
domain/usecase (GetChannelsUseCase, SaveCustomPlaylistUrlUseCase, ...)
        │  depends on
        ▼
domain/repository (PlaylistRepository interface — pure Kotlin)
        ▲  implements
        │
data/repository (PlaylistRepositoryImpl — Context, SharedPreferences, asset/URL loading)
```

## Personal-use features (beyond the original MVP)

- **Custom M3U URL**, entered via a Leanback `GuidedStepSupportFragment`
  (TV's standard D-pad-friendly text entry widget) and persisted across
  restarts. Falls back to the bundled test playlist if the saved URL fails
  to load, so a flaky personal server never leaves you on a blank screen.
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

## Tech stack & why

| Choice | Why |
|---|---|
| **Leanback** (`androidx.leanback`) for browsing | Purpose-built Android TV framework — D-pad focus movement, scaling/elevation on focus, and grid scrolling are handled by the framework, not hand-rolled key listeners. This is what "remote-friendly navigation" means in practice on TV. |
| **Media3 ExoPlayer** for playback | Hardware-accelerated decode and adaptive bitrate (HLS) across wildly different TV hardware tiers — the standard choice for smooth streaming playback on Android. |
| **Glide** for channel logos | Grid cells get recycled continuously as D-pad focus moves; Glide's bitmap pool avoids reallocating bitmaps on every scroll step, which is what keeps focus movement from dropping frames on lower-end TV boxes. |
| **Tuned `DefaultLoadControl`** | See below — this is the actual mechanism behind "no buffering." |

## How the buffering tuning works

`PlaybackActivity` configures ExoPlayer's `LoadControl` with:
- `minBufferMs = 15_000`, `maxBufferMs = 50_000` — the player keeps a large
  cushion of already-downloaded media, so short network hiccups don't empty
  the buffer and cause a visible stall.
- `bufferForPlaybackAfterRebufferMs = 5_000` — if a stall *does* happen, the
  player waits for a solid 5-second cushion before resuming, rather than
  resuming immediately and stalling again a second later.

The trade-off is a slightly longer initial buffering screen (a couple of
seconds) in exchange for far fewer stalls once playback is running — the
right trade for a "channel" experience where nobody is scrubbing a
timeline.

## What's in the MVP

- M3U parsing (`data/m3u/M3UParser.kt`) — tolerant of malformed lines, reads
  `tvg-logo` / `group-title` attributes
- Zero-config launch: bundled playlist loads automatically, no setup screen
- Single-grid channel browser (Leanback `VerticalGridSupportFragment`)
- Full-screen playback with a tuned ExoPlayer buffer, Back-button exit

## Deliberately out of scope for this MVP

- **Adding/editing playlist URLs from the UI** — `loadPlaylistFromUrl()`
  exists in the repository but isn't wired to a settings screen yet
- **EPG / program guide** — this is a channel-switcher, not a guide
- **Favorites, search, channel groups as separate rows** — the single flat
  grid is intentional for "most simplified interface"; grouping by
  `group-title` (already parsed) is a natural next step once there's a
  real multi-hundred-channel playlist to organize
- **Persisted playback position / catch-up TV** — this targets live streams

## Setup

Open in Android Studio, sync Gradle, run on an Android TV emulator or
device (or a Fire TV / Google TV box in developer mode). No API keys or
backend needed — the bundled playlist plays immediately.
