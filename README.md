# Android_MicroClimateFinder

## Build variants

The app has two build flavor dimensions:

| Dimension | Flavors | Meaning |
|---|---|---|
| `edition` | `core` (default), `demo` | **core** is the app alone, from the start screen on. **demo** adds the *Demos & Previews* hub in the menu and the legacy and prototype screens behind it (standalone Topography and Elevation, draggable maps, the Bay Area transect, animation previews, the station card preview). |
| `distribution` | `foss` (default), `commercial` | Who publishes the build (different application ids). |

Run the default variant (`coreFossDebug`) for a clean app with nothing else. Pick `demoFossDebug` in
Android Studio's *Build Variants* panel to see the demos. Edition-specific code lives in `app/src/core`
and `app/src/demo`; `app/src/main` is shared by both.
