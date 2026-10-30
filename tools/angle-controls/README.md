# Angle controls, kept as a toolbox

This folder is **not part of the app build**: it is not in any Gradle module, so nothing here is compiled or shipped. It keeps two
earlier ways to turn the Elevation Bands' viewing angle, for comparing against the quarter dial the app uses now
(`BandAngleArc.kt`, with the heading-up `BandCompass.kt`). Your IDE may show these files with unresolved references until you copy
them into the app; that is expected.

| File | What it is |
|---|---|
| `BandAngleSlider.kt` | A thin horizontal slider, west at the left end and north at the right, with a round thumb, a tick at each detent, snapping at 0, 45 and 90 degrees and the same haptics as the dial. Takes the angle and reports changes. |
| `BandHeadingStrip.kt` | A compass strip like a boat's heading bar: the compass names scroll sideways under a fixed red mark. A picture of the heading only, with no touches. It went above the slider. |

Both take the viewing angle as whole degrees (0 north, 90 west, see `BandAngle`) and use the app's `AngleHaptics`, strings and
`BandAngle` helpers, which stay in the app.

## Trying one in the app

1. Copy the file(s) into `app/src/main/java/io/github/genesjc/climatefinder/ui/components/`. The package line already matches.
2. In `ElevationProfileCanvas.kt`, in place of the `BandAngleArc(...)` block, add the control (the key is `layerLegendWidthDp(true)` wide,
   and the slider and strip use that width). Slider only:

   ```kotlin
   BandAngleSlider(
       angle = bandAngle,
       onAngleChange = onAngleChange,
       modifier = Modifier
           .align(Alignment.BottomEnd)
           .padding(end = LEGEND_MARGIN_DP.dp, bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP).dp)
           .width(layerLegendWidthDp(true).dp)
   )
   ```

   With the strip above it:

   ```kotlin
   BandHeadingStrip(
       angle = bandAngle,
       modifier = Modifier
           .align(Alignment.BottomEnd)
           .padding(
               end = LEGEND_MARGIN_DP.dp,
               bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP + BAND_ANGLE_SLIDER_HEIGHT_DP + BAND_HEADING_GAP_DP).dp
           )
           .width(layerLegendWidthDp(true).dp)
   )
   ```

3. Keep the labels clear of the control: the `taken` list in `layOut` holds a rectangle for each control (the dial's is the
   `the quarter dial` entry); give the slider and strip rectangles of their own heights above the key, and move the direction note
   (`BandAngleToast`) up by their height, since it sits above the key to the left of the dial.
4. The compass at the top right (`BandCompass`) is independent of the control and can stay with either.

The strings these use are still in `strings_ui.xml` and `strings_accessibility.xml`
(`elevation_angle_end_*`, `elevation_heading_*`, `elevation_angle_slider`, `elevation_angle_state`).
