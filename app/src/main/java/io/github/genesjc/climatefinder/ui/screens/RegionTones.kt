package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.graphics.Color

/*
 * The tones the Home folder tab draws from one region colour (Gene, 2026-11-04): the tab's tint (in LandingScreen), the name's ink,
 * and the view chip's fill. A widget in the tab takes its colours from here, so the tab follows one scheme whatever the region.
 */

/** The brightness of the name's ink (0 is black, 1 full): low, so it is dark and reads on the tint. */
private const val TAB_INK_VALUE = 0.44f

/** How much the colour's saturation is multiplied for the name's ink, so a muted region colour does not go grey when it is darkened. */
private const val TAB_INK_SATURATION_BOOST = 2.0f

/**
 * The name's ink for a region colour: the same hue, darkened to [TAB_INK_VALUE] with its saturation lifted by [TAB_INK_SATURATION_BOOST]
 * (capped at 0.9), so Los Angeles gets a deep navy and the Bay Area a deep green. Fading the colour toward black instead (the first
 * try, build 149.62) kept it so desaturated that it looked plain black.
 */
internal fun regionInk(colorHex: String): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colorHex), hsv)
    hsv[1] = (hsv[1] * TAB_INK_SATURATION_BOOST).coerceAtMost(0.9f)
    hsv[2] = TAB_INK_VALUE
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** The brightness of the view chip's fill (a deeper, richer shade than the ink's would be on its own, since white text sits on it). */
private const val CHIP_FILL_VALUE = 0.55f

/** How much the saturation is multiplied for the chip's fill. */
private const val CHIP_FILL_SATURATION_BOOST = 1.7f

/**
 * The view chip's fill for a region colour: the same hue as the tab and the name's ink, saturated and deep enough to carry white
 * bold text (the look of the README's chips). Los Angeles gets a steel navy, the Bay Area a teal green, Central Valley a rich brown.
 */
internal fun regionChipFill(colorHex: String): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colorHex), hsv)
    hsv[1] = (hsv[1] * CHIP_FILL_SATURATION_BOOST).coerceAtMost(0.9f)
    hsv[2] = CHIP_FILL_VALUE
    return Color(android.graphics.Color.HSVToColor(hsv))
}
