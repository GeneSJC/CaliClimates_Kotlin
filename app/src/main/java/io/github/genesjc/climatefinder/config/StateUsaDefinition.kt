package io.github.genesjc.climatefinder.config

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.domain.model.GeoBounds
import io.github.genesjc.climatefinder.domain.model.Landmark
import io.github.genesjc.climatefinder.domain.model.StateUsaOutline
import io.github.genesjc.climatefinder.domain.model.TreeKind

/**
 * Everything that differs from one USA state to the next: its regions, outline, picture bounds, station packs,
 * landmarks, scenery and the picker map's artwork. The app asks the registered definition (Koin, and
 * `LocalStateUsa` in the screens); nothing outside the definition's own object names a state. Only California is
 * defined today ([StateUsaCA]).
 */
interface StateUsaDefinition {
    /** The state's short code, for example "ca". */
    val code: String

    val regions: List<StateUsaRegion>

    /** The region the app opens on when nothing valid is saved. */
    val defaultRegionId: String

    /** The line under the app's name on the opening view and in the About dialog. */
    @get:StringRes
    val taglineRes: Int

    /** The title of the Regional Sections screen. */
    @get:StringRes
    val regionsScreenTitleRes: Int

    /** The title of the map card on the Regional Sections screen. */
    @get:StringRes
    val regionsMapTitleRes: Int

    /** The quiet line under the region picker's title (for example how many regions the state has). */
    @get:StringRes
    val regionPickerSubtitleRes: Int

    val outline: StateUsaOutline

    /**
     * Decorative dots for the opening view: (longitude, latitude) points scattered inside the outline, to say "weather stations on a
     * map". They are not real stations and are not read from any data; a state with none shows just its outline.
     */
    val introDots: List<Pair<Double, Double>> get() = emptyList()

    /** The ground each region's terrain picture covers (the images are generated to these bounds). */
    val terrainBounds: Map<String, GeoBounds>

    /** The ground the statewide pictures cover. */
    val stateBounds: GeoBounds

    /** The station pack (a file in the app's assets) of each region, by region id. */
    val stationPacks: Map<String, String>

    /** The town each station is in, by station id; a station without an entry is named from its own station name. */
    val stationTowns: Map<String, String>

    /** Landmarks to compare a height with, by region id. */
    val landmarks: Map<String, List<Landmark>>

    /** The trees along the near hills, by region id (repeats make a kind more common). */
    val treeMix: Map<String, List<TreeKind>>

    /**
     * The stations that stand for each region's best-known places, by region id, in the order they should be labelled: the
     * Elevation view shows them first whenever they are in view. Each id is a station in that region's pack.
     */
    val landmarkStations: Map<String, List<String>> get() = emptyMap()

    /** The regions as (longitude, latitude) polygons that share their boundary points, by region id. */
    val regionShapes: Map<String, List<Pair<Float, Float>>>

    val pickerMap: StateUsaPickerMap

    /** The region with this id, or null for an id that is not one of the state's regions. */
    fun region(id: String): StateUsaRegion? = regions.firstOrNull { it.id == id }

    /** The default region. */
    fun defaultRegion(): StateUsaRegion = region(defaultRegionId) ?: regions.first()
}
