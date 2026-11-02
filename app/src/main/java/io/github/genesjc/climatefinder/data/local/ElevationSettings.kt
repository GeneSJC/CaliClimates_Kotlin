package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.BandAngle

/** What the app remembers about the Elevation view, between launches: its hint, its layers and the bands' direction. */
interface ElevationSettings {
    /** Counts this launch once, however many times the Elevation view is opened during it. */
    fun countElevationHintLaunch()

    /** The "tap the line" hint shows on each of the first few launches, and stops at the first tap. */
    fun shouldShowElevationHint(): Boolean

    fun markElevationLineTapped()

    /** Whether the Elevation view shows its layers (the bands). It starts on: the bands are the view the app is built around, and the single line is one tap away. */
    fun elevationLayers(): Boolean

    fun saveElevationLayers(on: Boolean)

    /**
     * The direction the Elevation Bands look across the region, in whole degrees from north (0, the start, as the bands
     * always ran) toward west (90); see [BandAngle].
     */
    fun elevationBandAngle(): Int

    fun saveElevationBandAngle(degrees: Int)
}
