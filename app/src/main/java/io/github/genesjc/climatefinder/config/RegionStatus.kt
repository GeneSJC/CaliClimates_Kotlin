package io.github.genesjc.climatefinder.config

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.R

enum class RegionStatus(@StringRes val labelRes: Int) {
    ACTIVE_TRANSECT(R.string.region_status_active),
    PLANNED_CONSTRAINTS(R.string.region_status_planned)
}
