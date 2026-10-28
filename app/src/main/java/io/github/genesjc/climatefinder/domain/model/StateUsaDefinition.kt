package io.github.genesjc.climatefinder.domain.model

import androidx.annotation.StringRes

/** A box of longitude and latitude. */
data class GeoBounds(val minLon: Double, val maxLon: Double, val minLat: Double, val maxLat: Double)

/** A well-known place and its height in feet (approximate), for comparisons in plain words. */
data class Landmark(val name: String, val feet: Int)

/** The kinds of tree drawn along the near hills of the Elevation view. */
enum class TreeKind { CONIFER, OAK, PALM }

/** A state's own outline as (longitude, latitude) pairs, for the maps that have no region artwork. */
data class StateUsaOutline(
    /** The sea coast, from the northern border to the southern one. */
    val coast: List<Pair<Double, Double>>,
    /** The land border, from the southern coast end round to the northern coast end. */
    val landBorder: List<Pair<Double, Double>>
)

/** A name drawn on the hand-drawn picker map: its string resource and where it sits. */
data class StateUsaMapLabel(
    @StringRes val textRes: Int,
    val lon: Float,
    val lat: Float,
    /** A bigger, heavier name (for the neighbour that matters most). */
    val emphasis: Boolean = false
)

/** The dashed line where the state meets its neighbours, from [fromLon] east to the map's edge. */
data class StateUsaBorderLine(val lat: Float, val fromLon: Float)

/** The artwork of the hand-drawn region picker map, in longitude and latitude. */
data class StateUsaPickerMap(
    /** The window the map shows. */
    val view: GeoBounds,
    /** The sea along the coast, from the map's top-left corner round to its bottom edge at [seaBottomLon]. */
    val seaPoints: List<Pair<Float, Float>>,
    val seaBottomLon: Float,
    /** Latitudes of the faint grid lines. */
    val gridLatitudes: List<Float>,
    /** Mountain ranges drawn as hachure lines when there is no terrain picture. */
    val ridges: List<List<Pair<Float, Float>>>,
    val borderLine: StateUsaBorderLine,
    /** Region names, drawn on pills. */
    val regionLabels: List<StateUsaMapLabel>,
    /** The neighbours, drawn quietly. */
    val neighbourLabels: List<StateUsaMapLabel>
)

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

    val outline: StateUsaOutline

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

    /** The regions as (longitude, latitude) polygons that share their boundary points, by region id. */
    val regionShapes: Map<String, List<Pair<Float, Float>>>

    val pickerMap: StateUsaPickerMap

    /** The region with this id, or null for an id that is not one of the state's regions. */
    fun region(id: String): StateUsaRegion? = regions.firstOrNull { it.id == id }

    /** The default region. */
    fun defaultRegion(): StateUsaRegion = region(defaultRegionId) ?: regions.first()
}
