package io.github.genesjc.climatefinder.data.local

import androidx.compose.ui.graphics.ImageBitmap

/** The picker's terrain: land inside the state, and the neighbouring land (faded). Same bounds. */
class StateUsaTerrain(val state: ImageBitmap, val surround: ImageBitmap)
