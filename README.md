<div align="center">

<img src="docs/images/banner.png" alt="Cali Climates: explore California's climates, region by region" width="100%">

<br>

**An Android app that shows how California's weather changes from the coast to the mountains to the desert.**<br>
Live National Weather Service readings from about 360 stations in six regions, on shaded-relief maps and an elevation chart.

<br>

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-1D4ED8?style=flat-square)
![Koin](https://img.shields.io/badge/DI-Koin-F2A50F?style=flat-square)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache_2.0-0B1736?style=flat-square)](LICENSE)

[**What it does**](#-what-it-does) &nbsp;·&nbsp; [**Screens**](#-screens) &nbsp;·&nbsp; [**Run it**](#-run-it) &nbsp;·&nbsp; [**Architecture**](#-architecture) &nbsp;·&nbsp; [**Tech stack**](#-tech-stack) &nbsp;·&nbsp; [**Docs**](#-docs) &nbsp;·&nbsp; [**Status**](#-status)

<img src="docs/images/divider.png" alt="" width="100%">

</div>

## ✨ What it does

California is many climates at once: fog on the coast, snow in the Sierra, desert heat an hour's drive inland. Cali Climates
shows that as it is right now. Pick one of six regions and see its weather stations on a relief map, with a real reading from
each, and how the numbers change as the ground climbs.

<table>
  <tr>
    <td width="33%" valign="top">
      <h4>🌡️ Live readings</h4>
      Temperature, humidity, wind and pressure from about 360 National Weather Service stations, fetched on demand and kept for a few minutes so the app stays quick and polite.
    </td>
    <td width="33%" valign="top">
      <h4>🗺️ Six regions</h4>
      From Northern California to San Diego, each with its own earth tone. Change region and the page color, the map and the relief behind it all move together.
    </td>
    <td width="33%" valign="top">
      <h4>⛰️ Topography map</h4>
      Stations on a shaded-relief picture of the region, with pinch, drag and double-tap zoom, and labels that appear as you zoom in.
    </td>
  </tr>
  <tr>
    <td valign="top">
      <h4>📈 Elevation view</h4>
      The same stations as a profile from valley floor to ridge, with layered distant ranges drawn from the real skyline, so you can see why the temperature changes.
    </td>
    <td valign="top">
      <h4>🪪 Station cards and lists</h4>
      A card per station with its terrain, height in feet, a plain-words band ("foothills", "high country") and a comparison with a well-known landmark.
    </td>
    <td valign="top">
      <h4>📳 A tactile region change</h4>
      A soft haptic tick, the page easing to the new region's color and the relief sliding to another part of the landscape: switching feels physical, not like a screen swap.
    </td>
  </tr>
</table>

> [!NOTE]
> Readings come from the National Weather Service's public API (`api.weather.gov`), which needs no key. A station that cannot
> report shows plainly that it has no reading, instead of a made-up number.

## 📱 Screens

<div align="center">

<table>
  <tr>
    <td align="center"><img src="docs/images/placeholder-home.png" alt="Home screen" width="230"><br><sub><b>Home</b><br>region, station tile, map</sub></td>
    <td align="center"><img src="docs/images/placeholder-elevation.png" alt="Elevation view" width="230"><br><sub><b>Elevation</b><br>ridges and the profile</sub></td>
    <td align="center"><img src="docs/images/placeholder-regions.png" alt="Region picker" width="230"><br><sub><b>Regions</b><br>tap a region on the map</sub></td>
  </tr>
</table>

<br>

<img src="docs/images/placeholder-demo.png" alt="Demo of the app" width="640">

</div>

<sub>Screenshots and a demo GIF are on the way. The images above are placeholders (see [`docs/images`](docs/images/README.md)).</sub>

## 🚀 Run it

**You need** Android Studio (current stable). The app runs on Android 7.0 (API 24) and newer.

```bash
git clone https://github.com/GeneSJC/Android_MicroClimateFinder.git
cd Android_MicroClimateFinder
```

1. Open the folder in Android Studio and let Gradle sync.
2. In the **Build Variants** panel keep the default, `coreFossDebug` (the app on its own, the open-source build).
3. Run it on a device or an emulator. The readings need an internet connection.

<details>
<summary><b>Command line and tests</b></summary>

```bash
# a debug APK of the default variant
./gradlew assembleCoreFossDebug

# unit tests (name both distributions: the bare task name is ambiguous with two flavor dimensions)
./gradlew testCoreFossDebugUnitTest testCoreCommercialDebugUnitTest
```

About 200 unit tests in 28 classes cover the ViewModels, the repositories, the live-readings holder, the NWS client (against a
local test server) and the station-file format. Two of them are rules written as tests: no user-facing word may be written
in code (every word is a string resource), and every USA state definition must satisfy the same checks.

</details>

## 🏗️ Architecture

One direction only, with every layer doing one job:

```mermaid
flowchart TB
    subgraph UI["🖼️ UI layer"]
        V["<b>Screen</b><br/>Compose"] --> VM["<b>ViewModel</b><br/>state + wording"]
    end
    subgraph DATA["🧠 Data layer"]
        VM --> R["<b>Repositories and holders</b><br/>stations, terrain, live readings, settings"]
        R --> DS["<b>Data sources</b><br/>assets, one HTTP call"]
    end
    DS --> NWS(["🌦️ NWS api.weather.gov"])
    DS --> A[("📦 station packs, terrain pictures")]
    K{{"💉 Koin<br/>supplies every dependency"}} -.-> VM
    K -.-> R
    classDef ui fill:#1D4ED8,stroke:#0B1736,color:#ffffff;
    classDef data fill:#0B1736,stroke:#38BDF8,color:#ffffff;
    classDef edge fill:#F59E0B,stroke:#0B1736,color:#0B1736;
    classDef di fill:#7057FF,stroke:#0B1736,color:#ffffff;
    class V,VM ui;
    class R,DS data;
    class NWS,A edge;
    class K di;
```

| Layer | Owns | Must not |
|---|---|---|
| **Screen** | Layout, and local UI state such as animations and open dialogs | Hold data, or call a repository |
| **ViewModel** | The state a screen shows, the wording, one-off events | Contain a feature's data logic, or know about files and the network |
| **Repositories and holders** | One kind of data each: stations (read once and kept), terrain pictures, live readings (one app-wide holder), the saved settings | Know about screens or wording |
| **Data sources** | One external thing: the station files, the terrain pictures, the NWS call | Decide what the app does with the data |

**One idea is not obvious: a USA state is one object.** Everything that differs from one state to the next (its regions,
outline, terrain bounds, station files, station towns, landmarks, map artwork and wording) sits behind one interface, so the
rest of the app never names California:

```mermaid
flowchart LR
    S["<b>Screens and ViewModels</b>"] --> D{{"<b>StateUsaDefinition</b><br/>one interface per USA state"}}
    L["<b>Data layer</b><br/>packs, terrain, towns, settings"] --> D
    D -.-> CA["<b>StateUsaCA</b><br/>the only state so far"]
    CA -.-> W["<b>ca_ strings</b><br/>the state's wording"]
    classDef a fill:#1D4ED8,stroke:#0B1736,color:#ffffff;
    classDef b fill:#0B1736,stroke:#38BDF8,color:#ffffff;
    classDef c fill:#F59E0B,stroke:#0B1736,color:#0B1736;
    class S,L a;
    class D c;
    class CA,W b;
```

### Principles at a glance

- 🧱 **Dependencies point one way.** Screens never reach a repository; the data layer never knows about screens.
- 💉 **Koin builds everything in one place** (`di/AppModule.kt`). ViewModels declare what they need as plain members and keep a no-argument constructor.
- 🔤 **Every user-facing word is a string resource,** kept in files by category. The data layers report facts (numbers, enums, resource ids); the screens and ViewModels choose the words. A test fails if a literal sneaks in.
- 🧪 **Everything is testable without a device.** Data sources sit behind small interfaces; tests register fakes with `restartKoin { single { fake } }`.
- 🌎 **One state, one interface.** A second USA state is a new definition object and its own strings, not a rework.
- 🅿️ **Parked work stays recoverable.** A feature that is not shipping is removed from the main line, tagged, and given a restore guide (see [Parked features](#parked-features)).

### Two editions, two distributions

| Dimension | Flavors | Meaning |
|---|---|---|
| `edition` | `core` (default), `demo` | **core** is the app alone. **demo** adds a *Demos & Previews* hub with legacy and prototype screens behind it. The demo edition is held back and does not build at the moment. |
| `distribution` | `foss` (default), `commercial` | Who publishes the build. `foss` is `io.github.genesjc.microclimatefinder` (version suffix `-foss`); `commercial` is `com.genetech.microclimatefinder` (suffix `-commercial`). The ids differ, so both install side by side. |

Run the default variant, `coreFossDebug`, for a clean app. Edition-specific code lives in `app/src/core` and `app/src/demo`,
distribution-specific code in `app/src/foss` and `app/src/commercial`; `app/src/main` is shared. Neither distribution
contains any advertising code or library today.

### Where things live

```text
app/src/main/java/io/github/genesjc/microclimatefinder/
├── data/         repositories, the live-readings holder, the NWS client, saved settings
├── di/           AppModule: the one place Koin is told how things are built
├── domain/       the plain model: stations, regions, the USA state definition, elevation facts
├── viewmodel/    the base ViewModel, the build config, the string resolver
└── ui/
    ├── components/   cards, the region picker, drawers, map controls
    ├── home/ map/ regionpicker/ root/ stationlist/   the ViewModels
    ├── screens/      Home, the maps, the Elevation view, the Station List, the region screen
    ├── text/         the helpers that turn facts into words
    └── theme/        colors, the backdrop, and the USA state handed to the screens

app/src/main/assets/   the station packs and the terrain pictures
app/src/main/res/values/   strings by category (strings_region_ca.xml is California's)
app/src/core/  demo/  foss/  commercial/   per-flavor code      docs/   restore guides and README art
```

## 🧰 Tech stack

| | |
|---|---|
| **Language** | Kotlin 2.2 |
| **UI** | Jetpack Compose (BOM 2026.02), Material 3, custom Canvas drawing for the maps and the elevation chart |
| **Architecture** | MVVM with repositories and a state holder for live readings; Koin 4.2 for dependency injection |
| **Networking** | Retrofit 3.0 over OkHttp 4.12, kotlinx.serialization |
| **Async** | Kotlin Coroutines and Flow (StateFlow for state, a Channel for one-off events) |
| **Build** | Android Gradle Plugin 9.4, two flavor dimensions, minSdk 24, targetSdk 37 |
| **Tests** | JUnit 4, coroutines-test, MockK and MockWebServer where an Android class or the network is involved; fakes elsewhere |

## 📚 Docs

| Read | To learn |
|---|---|
| [`docs/ads-restore.md`](docs/ads-restore.md) | How to bring the parked ads work back, step by step |
| [`docs/news-restore.md`](docs/news-restore.md) | What the parked local-news feature was and how to bring it back |
| [`docs/images/README.md`](docs/images/README.md) | The README artwork: what each image is and how to regenerate it |

## 🗺️ Status

| | |
|---|---|
| ✅ **Done** | Layered MVVM with Koin, string resources for every word, a USA state definition, live readings in one app-wide holder, Retrofit for the weather call, one settings store, a region change that never blocks the main thread |
| ⏸️ **Parked** | Ads (AdMob banner and a one-hour ad-free reward) and a local-news block, each kept recoverable by a tag and a restore guide |
| 🔜 **Next** | A first release (store listing, privacy policy, signing), real screenshots for this page, and continuous integration |
| 💡 **Later** | A second USA state, localized place names, search on the Station List |

<a id="parked-features"></a>

## ⏸️ Parked features

Two features were built or started and then taken out of the main line so the code base only holds what ships. Each is
kept by an annotated git tag, and has a restore guide in this repository (Markdown, in `docs/`) that says what is in the
tag, what is missing, and how to bring it back onto a fresh branch from `develop`. Do not merge a tag: copy files from it.

| Feature | Tag | Restore guide | State |
|---|---|---|---|
| **Ads** (AdMob banner, consent, an ad-free hour for a rewarded video) | `ads-parked-2026-10-27` | [docs/ads-restore.md](docs/ads-restore.md) | Not on `develop`. The app is not published with ads for now, so the `commercial` build is ads-free: no ads code, library or manifest entry. The tag holds an SDK-free seam, the Gradle `adsEnabled` switch, tests, the foss-clean script and a copy of the ads kit. Written, never built; the real AdMob controller was not written. |
| **Local news** (a news block and story dialog on the station card) | `news-parked-2026-10-27` | [docs/news-restore.md](docs/news-restore.md) | Removed at Build 98.1. It was never shown to users (sample stories, behind a switch that was off). The tag is `develop` at Build 96.1, the last commit that had it, and it worked. |

Read a file from a tag without checking it out: `git show <tag>:<path>`. The station-to-town naming that used to live
beside the news code stays in the app, in `StationTowns`.

## 📄 License

Apache License 2.0; see [`LICENSE`](LICENSE).

Weather readings come from the U.S. National Weather Service (`api.weather.gov`). The terrain pictures are built from public
elevation data (USGS 3DEP and NASA SRTM, through AWS Terrain Tiles). Cali Climates is an independent project and is not
affiliated with or endorsed by the National Weather Service, USGS or NASA.

<div align="center">

<img src="docs/images/divider.png" alt="" width="100%">

<br>

<img src="docs/images/icon.png" alt="The Cali Climates icon" width="90">

<sub>Built with Kotlin, Compose, a lot of small commits, and an interest in microclimates.</sub>

</div>
