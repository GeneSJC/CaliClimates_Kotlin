package io.github.genesjc.climatefinder.data.local

import androidx.compose.ui.graphics.ImageBitmap
import io.github.genesjc.climatefinder.domain.model.GeoBounds

/** A shaded-relief picture of a region and the ground it covers. Its pixels are linear in lon and lat. */
class TerrainImage(val image: ImageBitmap, val bounds: GeoBounds)
