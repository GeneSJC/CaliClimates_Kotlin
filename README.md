# Cali Climates

## Highlights

- **A tactile feel when you change region.** The app responds to a region change on several levels at once, so
  switching feels physical rather than like a screen swap: a soft haptic tick on the phone, the page colour
  easing to the new region's earth tone, and the relief texture behind everything sliding over to a different part of
  the landscape (the Topography map and the page behind it read as one continuous surface). *(Add a short recording here.)*

## Build variants

The app has two build flavor dimensions:

| Dimension | Flavors | Meaning |
|---|---|---|
| `edition` | `core` (default), `demo` | **core** is the app alone, from the start screen on. **demo** adds the *Demos & Previews* hub in the menu and the legacy and prototype screens behind it (standalone Topography and Elevation, draggable maps, the Bay Area transect, animation previews, the station card preview). |
| `distribution` | `foss` (default), `commercial` | Who publishes the build (different application ids). |

Run the default variant (`coreFossDebug`) for a clean app with nothing else. Pick `demoFossDebug` in
Android Studio's *Build Variants* panel to see the demos. Edition-specific code lives in `app/src/core`
and `app/src/demo`; `app/src/main` is shared by both.

## Parked features

Two features were built or started and then taken out of the main line so the code base only holds what ships. Each is
kept by an annotated git tag, and has a restore guide in this repository (Markdown, in `docs/`) that says what is in the
tag, what is missing, and how to bring it back onto a fresh branch from `develop`. Do not merge a tag: copy files from it.

| Feature | Tag | Restore guide | State |
|---|---|---|---|
| **Ads** (AdMob banner, consent, an ad-free hour for a rewarded video) | `ads-parked-2026-10-27` | [docs/ads-restore.md](docs/ads-restore.md) | Not on `develop`. The app is not published with ads for now, so the `commercial` build is ads-free: no ads code, library or manifest entry. The tag holds an SDK-free seam, the Gradle `adsEnabled` switch, tests, the foss-clean script and a copy of the ads kit. Written, never built; the real AdMob controller was not written. |
| **Local news** (a news block and story dialog on the station card) | `news-parked-2026-10-27` | [docs/news-restore.md](docs/news-restore.md) | Removed at Build 98.1. It was never shown to users (sample stories, behind a switch that was off). The tag is `develop` at Build 96.1, the last commit that had it, and it worked. |

Read a file from a tag without checking it out: `git show <tag>:<path>`. The station-to-town naming that used to live
beside the news code stays in the app, in `StationTowns`.
