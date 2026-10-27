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

### Ads

The `foss` build never has ads: it contains no ads library, no ads manifest entry and no advertising-id permission.
The `commercial` build carries the ads code, but **ads are off by default** (the app is not published with ads for
now). While they are off the commercial build binds a controller that does nothing: no consent form, no ad
requests, no banner, no ad-free offer. To turn them on for a local build, put `adsEnabled=true` in `local.properties`
(or in `~/.gradle/gradle.properties`, or pass `-PadsEnabled=true`). The build prints whether ads are on or off at the
start of every build.
