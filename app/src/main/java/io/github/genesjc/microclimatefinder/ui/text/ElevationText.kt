package io.github.genesjc.microclimatefinder.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.domain.Elevation
import io.github.genesjc.microclimatefinder.ui.theme.LocalStateUsa

/** "1,115 ft" */
@Composable
fun elevationFeetText(meters: Int): String = stringResource(R.string.elevation_feet, Elevation.feet(meters))

/** "1,115 ft · Foothills" */
@Composable
fun elevationDescribeText(meters: Int): String =
    stringResource(R.string.elevation_described, elevationFeetText(meters), stringResource(Elevation.band(meters).labelRes))

/** "66–9,678 ft" */
@Composable
fun elevationRangeText(minMeters: Int, maxMeters: Int): String =
    stringResource(R.string.elevation_range, Elevation.feet(minMeters), Elevation.feet(maxMeters))

/**
 * A sentence about the height: a landmark comparison, when there is one, and the rule-of-thumb cooling. The facts
 * come from [Elevation.note]; the wording is the string resources.
 */
@Composable
fun elevationNoteText(meters: Int, regionId: String?): String {
    val landmarks = LocalStateUsa.current.landmarks[regionId].orEmpty()
    val note = Elevation.note(meters, landmarks)
    val landmark = note.landmark?.let {
        if (it.isNear) stringResource(R.string.elevation_note_about, it.name, it.feet)
        else stringResource(R.string.elevation_note_higher, it.name, it.feet)
    }
    val cooling = if (note.coolerFahrenheit >= 1) {
        stringResource(R.string.elevation_note_cooler, note.coolerFahrenheit)
    } else {
        stringResource(R.string.elevation_note_sea_level)
    }
    return if (landmark != null) stringResource(R.string.elevation_note_joined, landmark, cooling) else cooling
}

/**
 * "1,115 ft" as a function, for text that is built in a `remember` or a draw lambda, where [stringResource] cannot be
 * called. The resources are read in the composable body, then captured.
 */
@Composable
fun rememberFeetFormatter(): (Int) -> String {
    val resources = LocalContext.current.resources
    return remember(resources) { { meters: Int -> resources.getString(R.string.elevation_feet, Elevation.feet(meters)) } }
}
