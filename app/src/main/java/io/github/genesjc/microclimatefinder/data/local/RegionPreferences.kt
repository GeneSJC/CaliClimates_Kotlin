package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions

/** Where the chosen region is kept between launches (a seam so tests can use a fake). */
interface SelectedRegionStore {
    /** The saved region id, or the default when nothing valid is saved. */
    fun load(): String

    fun save(regionId: String)
}

/** Remembers the region the user picked, so the app reopens on it. */
class RegionPreferences(context: Context) : SelectedRegionStore {

    private val prefs = context.getSharedPreferences("region_prefs", Context.MODE_PRIVATE)

    override fun load(): String {
        val saved = prefs.getString(KEY_SELECTED_REGION, null)
        return if (CaliforniaRegionalDefinitions.ALL_REGIONS.any { it.id == saved }) {
            saved!!
        } else {
            CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id
        }
    }

    override fun save(regionId: String) {
        prefs.edit().putString(KEY_SELECTED_REGION, regionId).apply()
    }

    private companion object {
        const val KEY_SELECTED_REGION = "selected_region"
    }
}
