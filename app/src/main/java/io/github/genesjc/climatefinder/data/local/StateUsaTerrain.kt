package io.github.genesjc.climatefinder.data.local

import android.graphics.Bitmap

/** The picker's terrain: land inside the state, and the neighbouring land (faded). Same bounds. */
class StateUsaTerrain(val state: Bitmap, val surround: Bitmap)
