package io.github.genesjc.climatefinder.domain.model

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
