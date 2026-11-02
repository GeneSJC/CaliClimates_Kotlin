package io.github.genesjc.climatefinder.data.local

import android.graphics.Bitmap
import io.github.genesjc.climatefinder.domain.model.GeoBounds

/** A shaded-relief picture of a region and the ground it covers. Its pixels are linear in lon and lat. */
class TerrainImage(val bitmap: Bitmap, val bounds: GeoBounds)
