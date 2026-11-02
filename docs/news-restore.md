# Local news, parked: what was removed and how to bring it back

**Status (2026-10-27).** The Local News feature is not part of the app. It was never shown to users: it sat behind a
switch that was off (`AppFeatures.LOCAL_NEWS = false`), its content was a hand-written sample archive plus generic
placeholder stories, and there was no live feed. It was removed from the main line so the code base only holds what ships,
and it is kept as a reference by a tag.

- **Tag:** `news-parked-2026-10-27` (annotated; never moves). It points at `develop` as it stood at Build 96.1, the last
  commit that still had the news code:
  `f2151d2597776c1b8ab0f28aa3122ce3f51076af` (merge of PR #96). That build was run on a device and worked, so the tag is a
  working reference. If the tag is ever lost, the SHA still works: `git show f2151d2:<path>`.
- The parking commit is Build 98.1. Compare with `git diff news-parked-2026-10-27 <parking commit>`.
- Restore by copying files from the tag onto a fresh branch from the then-current `develop`, then re-adding the small
  edits below. Do not merge the tag.

## What the feature was

On the station card (full card, not the compact one) a "Local News" row (newspaper icon, "Local News", the town, a story
count and an arrow) opened a nearly full-screen dialog listing story cards (title, source, age, snippet, "Read story").
Tapping a story would open it. In the demo edition a story opened in an in-app web viewer screen. The stories came from
`getLocalStories(town)`: a hand-written archive for ten towns (Vacaville, Fairfield, Sacramento, Davis, Auburn, Monterey,
Salinas, Palo Alto, Half Moon Bay, Santa Cruz), and for every other town three templated placeholder stories with links
to a news search. The stories were samples with invented sources; none were live.

## What was removed (paths are from `app/src/`)

| Path | What it was |
|---|---|
| `main/.../data/local/LocalNewsRepository.kt` | The sample archive and `getLocalStories`. **Not deleted as a whole:** the station-to-town table and the name cleaning in the same file stayed, and moved to `domain/StationNameFormatter.kt` (first as `data/local/StationTowns.kt`). |
| `main/.../domain/model/LocalNewsStory.kt` | The story model: title, source, url, published, snippet. |
| `main/.../AppFeatures.kt` | The `LOCAL_NEWS` switch (the only switch it held). |
| `main/.../ui/components/StationCard.kt` | The Local News row, the story dialog, the `onOpenStory` parameter and the `cleanCity` and `newsStories` values. |
| `main/.../ui/navigation/AppScreen.kt` | The `NEWS_STORY_VIEWER` entry. |
| `main/res/values/strings_ui.xml`, `strings_messages.xml` | The news strings, below. |
| `demo/.../ui/screens/NewsStoryViewerScreen.kt` | The demo's in-app web viewer for a story. |
| `demo/.../EditionFlavor.kt`, `ui/screens/UnifiedDraggableMapScreen.kt`, `DemosAndPreviewsScreen.kt` | The demo wiring that opened a story in the viewer, and one sentence of a description. |

The strings, verbatim:

```xml
<!-- strings_ui.xml -->
<string name="news_title">Local News</string>
<string name="news_subtitle">%1$s • What\'s happening in this location</string>
<string name="news_source">Google News RSS • %1$s Area</string>
<string name="news_tap_to_open">Tap to open in app viewer</string>
<string name="news_read_story">Read story</string>

<!-- strings_messages.xml -->
<plurals name="news_story_count">
    <item quantity="one">%1$d story</item>
    <item quantity="other">%1$d stories</item>
</plurals>
```

## How to restore

> [!IMPORTANT]
> **Package fix-up for files copied from the tag.** The tag's files and folders use the package
> `io.github.genesjc.microclimatefinder`; this repository's package is `io.github.genesjc.climatefinder`. The paths in the
> commands below are the tag's paths. After copying, move the files under `.../genesjc/climatefinder/` and fix the package
> name in them, for example (from the repository root, after the `git checkout` below):
> ```
> for d in $(find app/src -type d -path "*/io/github/genesjc/microclimatefinder"); do
>   mkdir -p "$(dirname "$d")/climatefinder" && cp -R "$d"/. "$(dirname "$d")/climatefinder/" && rm -rf "$d"
> done
> grep -rl "io.github.genesjc.microclimatefinder" app scripts 2>/dev/null | xargs sed -i 's/io\.github\.genesjc\.microclimatefinder/io.github.genesjc.climatefinder/g'
> ```
> The kit under `docs/ads-kit/` uses placeholders (`PACKAGE_PATH`, `__PACKAGE__`), so it takes this repository's package directly.

1. Branch from the latest `develop`: `git checkout develop && git pull && git checkout -b feature-set/localNews`.
2. Copy the removed files from the tag:
   ```
   git checkout news-parked-2026-10-27 -- \
     app/src/main/java/io/github/genesjc/microclimatefinder/domain/model/LocalNewsStory.kt \
     app/src/main/java/io/github/genesjc/microclimatefinder/AppFeatures.kt \
     app/src/demo/java/io/github/genesjc/microclimatefinder/ui/screens/NewsStoryViewerScreen.kt
   ```
3. `LocalNewsRepository.kt`: do not overwrite. Create a news-only file (for example `data/local/LocalNewsSource.kt`) from the
   tag's `getLocalStories` and `ARCHIVE` (everything after `getLocalStories` in
   `git show news-parked-2026-10-27:app/src/main/java/io/github/genesjc/microclimatefinder/data/local/LocalNewsRepository.kt`),
   and have it take the town as a parameter, as before. The town now comes from `StationNameFormatter.cleanCityName`.
4. Re-add the strings above (the repository's string rules apply: wording in the category files).
5. Re-add the `NEWS_STORY_VIEWER` entry to `AppScreen`.
6. `StationCard.kt`: the removed blocks are in the tag; read them with
   `git show news-parked-2026-10-27:app/src/main/java/io/github/genesjc/microclimatefinder/ui/components/StationCard.kt`
   (the `if (AppFeatures.LOCAL_NEWS) { ... }` row inside the full card, and the `if (AppFeatures.LOCAL_NEWS && showNewsDialog)`
   dialog after it). Put them back, with `var showNewsDialog`, `cleanCity`, `newsStories` and `onOpenStory`. Today's card has
   moved on, so merge by hand and re-check the layout.
7. Demo edition: re-add the viewer wiring from the tag if the demo edition is wanted (it also needs the demo catch-up).
8. Build both flavors, run the unit tests, and check the card on a device with `LOCAL_NEWS` set to true.

## Before it comes back (why it was parked)

- It needs real content. The sample stories are invented, with made-up sources and ages. A real feature needs a source (the
  strings mentioned Google News RSS, which was never wired), the right to show and link it, handling for no network or no
  results, and a decision about where stories are fetched (a repository behind a small interface, like the live readings).
- It names California in its placeholder text and links. A state-agnostic version would take its wording and sources from the
  state definition, as the rest of the app now does.
- A privacy and permissions check (a story link opens a web page; the viewer was a web view).
